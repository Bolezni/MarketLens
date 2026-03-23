package com.bolezni.mvp_test.authorization.api.security.bruteforce;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Брутфорс-защита на логин:
 * 5 неудачных попыток -> блок на 15 минут.
 *
 * Хранение в памяти (Bucket4j + Caffeine): для multi-instance нужен Redis (bucket4j-redis).
 */
@Slf4j
@Service
public class LoginBruteForceService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build();

    public OptionalLong checkBlocked(String email) {
        String key = normalizeKey(email);
        Bucket bucket = buckets.get(key, k -> newBucket());

        long availableTokens = bucket.getAvailableTokens();
        if (availableTokens > 0) {
            return OptionalLong.empty();
        }

        // Раз блок уже активен, попытка потребления не должна уменьшить количество токенов,
        // но даст время до пополнения.
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.info("Login locked for key={} retryAfterSeconds={}", key, retryAfterSeconds);
        return OptionalLong.of(retryAfterSeconds);
    }

    public void onSuccess(String email) {
        buckets.invalidate(normalizeKey(email));
    }

    /**
     * Регистрирует неудачную попытку логина.
     * Возвращает {@code OptionalLong} с retry-after, если уже был достигнут лимит.
     */
    public OptionalLong onFailure(String email) {
        String key = normalizeKey(email);
        Bucket bucket = buckets.get(key, k -> newBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return OptionalLong.empty();
        }

        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        log.info("Login locked for key={} retryAfterSeconds={}", key, retryAfterSeconds);
        return OptionalLong.of(retryAfterSeconds);
    }

    private static Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        MAX_FAILED_ATTEMPTS,
                        Refill.intervally(MAX_FAILED_ATTEMPTS, LOCK_DURATION)
                ))
                .build();
    }

    private static String normalizeKey(String email) {
        if (email == null) {
            return "unknown";
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return "unknown";
        }
        return "login:" + trimmed.toLowerCase(Locale.ROOT);
    }
}

