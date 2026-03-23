package com.bolezni.mvp_test.authorization.api.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 100 запросов в минуту на ключ: для аутентифицированного пользователя — по {@link Authentication#getName()},
 * иначе — по IP (подходит для {@code /api/auth/**}).
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .build();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String key = resolveRateLimitKey(request);
        Bucket bucket = buckets.get(key, k -> newBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
            log.warn("Rate limit exceeded for key={}", key);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(CAPACITY));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        filterChain.doFilter(request, response);
    }

    private static String resolveRateLimitKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }
        return "ip:" + clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, REFILL_PERIOD)))
                .build();
    }
}
