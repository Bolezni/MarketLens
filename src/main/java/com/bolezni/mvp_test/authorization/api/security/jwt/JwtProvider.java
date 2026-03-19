package com.bolezni.mvp_test.authorization.api.security.jwt;

import io.jsonwebtoken.Claims;
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
import java.util.function.Function;

@Slf4j
@Component
public class JwtProvider {

    @Value("${spring.security.jwt.secret-key}")
    private String secret;

    @Value("${spring.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${spring.security.jwt.refresh.expiration}")
    private long refreshExpiration;

    public String buildRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshExpiration);
    }

    public String buildToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpiration);
    }

    public boolean isValidToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractEmail(String token) {
        return extractClaim(Claims::getSubject, token);
    }

    private boolean isValid(String token) {
        try {
            log.debug("🔐 Validating JWT token: {}", token.substring(0, Math.min(20, token.length())) + "...");

            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("🔐 Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (JwtException e) {
            log.error("🔐 JWT validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("🔐 JWT token is null or empty");
            return false;
        } catch (Exception e) {
            log.error("🔐 Unexpected error during token validation: {}", e.getMessage());
            return false;
        }
    }

    private String buildToken(UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .issuedAt(new Date(System.currentTimeMillis()))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }


    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
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
