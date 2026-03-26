package com.bolezni.mvp_test.authorization.api.security.jwt;

import com.bolezni.mvp_test.authorization.api.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtProvider {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_VERSION = "tokenVersion";

    @Value("${spring.security.jwt.secret-key}")
    private String secret;

    @Value("${spring.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${spring.security.jwt.refresh.expiration}")
    private long refreshExpiration;

    public String buildRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshExpiration, TOKEN_TYPE_REFRESH);
    }

    public String buildToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpiration, TOKEN_TYPE_ACCESS);
    }

    public boolean isValidToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equalsIgnoreCase(userDetails.getUsername()) && isTokenExpired(token));
    }

    public String extractEmail(String token) {
        return extractClaim(Claims::getSubject, token);
    }

    public String extractTokenType(String token) {
        return extractClaim(c -> c.get(CLAIM_TOKEN_TYPE, String.class), token);
    }

    public Date extractExpiration(String token) {
        return extractClaim(Claims::getExpiration, token);
    }

    public int extractTokenVersion(String token) {
        return extractClaim(c -> c.get(TOKEN_VERSION, Integer.class), token);
    }

    private String buildToken(UserDetails userDetails, long expiration, String tokenType) {
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        int tokenVersion = customUserDetails.userEntity().getTokenVersion();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of(CLAIM_TOKEN_TYPE, tokenType,
                        Claims.ID, UUID.randomUUID().toString(),
                        TOKEN_VERSION, tokenVersion))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .issuedAt(new Date(System.currentTimeMillis()))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }


    public boolean isTokenExpired(String token) {
        try {
            return getExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // Токен истёк — это и есть ответ
        } catch (JwtException e) {
            return true; // Невалидный токен считаем истёкшим
        }
    }


    private Date getExpiration(String token) {
        return extractClaim(Claims::getExpiration, token);
    }

    private <T> T extractClaim(Function<Claims, T> claimsExtractor, String token) {
        final Claims claims = extractAllClaims(token);
        return claimsExtractor.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
