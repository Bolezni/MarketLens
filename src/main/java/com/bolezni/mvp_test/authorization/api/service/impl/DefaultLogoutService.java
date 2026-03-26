package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.LogoutRequest;
import com.bolezni.mvp_test.authorization.api.service.LogoutService;
import com.bolezni.mvp_test.authorization.api.service.RefreshService;
import com.bolezni.mvp_test.authorization.store.RefreshTokenEntity;
import com.bolezni.mvp_test.authorization.store.RefreshTokenRepository;
import com.bolezni.mvp_test.authorization.store.UserEntity;
import com.bolezni.mvp_test.authorization.store.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DefaultLogoutService implements LogoutService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshService refreshService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        if (request == null || request.refreshToken() == null) {
            throw new IllegalArgumentException("Invalid request");
        }

        //Выгрузить токен из бд, проверить что он актуален, затем найти все токена и обнулить их
        String refreshToken = request.refreshToken().trim();

        refreshService.validateRefreshToken(refreshToken);

        String tokenHash = sha256Hex(refreshToken);
        RefreshTokenEntity refreshTokenEntity = refreshService.getRefreshTokenByTokenHash(tokenHash);

        UserEntity currentUser = refreshTokenEntity.getUser();

        userRepository.incrementTokenVersion(currentUser.getId());
        //обнуляться все токены
        revokeAllRefreshTokens(currentUser);
    }

    private void revokeAllRefreshTokens(UserEntity user) {
        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user);
        for (RefreshTokenEntity t : activeTokens) {
            t.setRevokedAt(LocalDateTime.now());
        }
        refreshTokenRepository.saveAll(activeTokens);
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
