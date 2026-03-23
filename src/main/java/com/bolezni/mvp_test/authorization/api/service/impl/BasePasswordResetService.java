package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.ForgotPasswordRequest;
import com.bolezni.mvp_test.authorization.api.dto.MessageResponse;
import com.bolezni.mvp_test.authorization.api.dto.ResetPasswordRequest;
import com.bolezni.mvp_test.authorization.api.service.PasswordResetEmailSender;
import com.bolezni.mvp_test.authorization.api.service.PasswordResetService;
import com.bolezni.mvp_test.authorization.store.PasswordResetTokenEntity;
import com.bolezni.mvp_test.authorization.store.PasswordResetTokenRepository;
import com.bolezni.mvp_test.authorization.store.RefreshTokenEntity;
import com.bolezni.mvp_test.authorization.store.RefreshTokenRepository;
import com.bolezni.mvp_test.authorization.store.UserEntity;
import com.bolezni.mvp_test.authorization.store.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class BasePasswordResetService implements PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailSender emailSender;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Куда вести ссылку из письма.
     * Обычно это фронтенд-страница, которая уже потом дергает API /reset-password.
     */
    @Value("${app.password-reset.ui-base-url:http://localhost:3000/reset-password?token=}")
    private String uiBaseUrl;

    @Override
    @Transactional
    public MessageResponse forgot(ForgotPasswordRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            return new MessageResponse("If account exists, password reset email was sent");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email).ifPresent(user -> {
            // Инвалидация всех активных токенов пользователя (просто и безопасно).
            List<PasswordResetTokenEntity> activeTokens =
                    tokenRepository.findAllByUserAndUsedAtIsNull(user);
            for (PasswordResetTokenEntity t : activeTokens) {
                t.setUsedAt(LocalDateTime.now());
            }
            tokenRepository.saveAll(activeTokens);

            String rawToken = generateToken();
            String tokenHash = sha256Hex(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES);

            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            tokenRepository.save(token);

            String resetLink = uiBaseUrl + rawToken;
            emailSender.sendPasswordResetEmail(user.getEmail(), resetLink);
        });

        // Важно: одинаковый ответ, чтобы нельзя было понять "существует ли email".
        return new MessageResponse("If account exists, password reset email was sent");
    }

    @Override
    @Transactional
    public MessageResponse reset(ResetPasswordRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is missing");
        }

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation does not match");
        }

        String tokenHash = sha256Hex(request.token().trim());
        PasswordResetTokenEntity token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token already used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }

        UserEntity user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        // Сломаем refresh токены после смены пароля.
        revokeAllRefreshTokens(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return new MessageResponse("Password updated");
    }

    private void revokeAllRefreshTokens(UserEntity user) {
        // Метод сейчас может отсутствовать — если не добавишь, просто закомменти.
        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user);
        for (RefreshTokenEntity t : activeTokens) {
            t.setRevokedAt(LocalDateTime.now());
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

