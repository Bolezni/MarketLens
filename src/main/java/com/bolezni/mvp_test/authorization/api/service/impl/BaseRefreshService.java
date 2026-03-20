package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.dto.RefreshRequest;
import com.bolezni.mvp_test.authorization.api.security.CustomUserDetails;
import com.bolezni.mvp_test.authorization.api.security.CustomUserDetailsService;
import com.bolezni.mvp_test.authorization.api.security.jwt.JwtProvider;
import com.bolezni.mvp_test.authorization.api.service.RefreshService;
import com.bolezni.mvp_test.authorization.store.RefreshTokenEntity;
import com.bolezni.mvp_test.authorization.store.RefreshTokenRepository;
import com.bolezni.mvp_test.authorization.store.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BaseRefreshService implements RefreshService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is missing");
        }

        String refreshToken = request.refreshToken().trim();

        String tokenType;
        try {
            tokenType = jwtProvider.extractTokenType(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e);
        }

        if (!JwtProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.ofInstant(
                    jwtProvider.extractExpiration(refreshToken).toInstant(),
                    ZoneId.systemDefault()
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e);
        }

        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String tokenHash = sha256Hex(refreshToken);
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (current.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
        }

        // дополнительно защищаемся от несостыковки в БД (если токен уже просрочен по записи)
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String email = jwtProvider.extractEmail(refreshToken).trim().toLowerCase(Locale.ROOT);
        UserEntity user = current.getUser();
        if (user == null || user.getEmail() == null || !user.getEmail().trim().toLowerCase(Locale.ROOT).equals(email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(user.getEmail());

        String newAccess = jwtProvider.buildToken(userDetails);
        String newRefresh = jwtProvider.buildRefreshToken(userDetails);

        RefreshTokenEntity replacement = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(sha256Hex(newRefresh))
                .expiresAt(LocalDateTime.ofInstant(jwtProvider.extractExpiration(newRefresh).toInstant(), ZoneId.systemDefault()))
                .build();
        refreshTokenRepository.save(replacement);

        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedBy(replacement);
        refreshTokenRepository.save(current);

        return new AuthResponse(newAccess, "Bearer", newRefresh);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}

