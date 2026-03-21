package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.service.EmailVerificationService;
import com.bolezni.mvp_test.authorization.store.EmailVerificationTokenEntity;
import com.bolezni.mvp_test.authorization.store.EmailVerificationTokenRepository;
import com.bolezni.mvp_test.authorization.store.UserEntity;
import com.bolezni.mvp_test.authorization.store.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BaseEmailVerificationService implements EmailVerificationService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.verification.base-url:http://localhost:8080/api/auth/verify-email}")
    private String verificationBaseUrl;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    @Override
    @Transactional
    public void createAndSendVerification(UserEntity user) {
        String rawToken = generateToken();
        EmailVerificationTokenEntity entity = EmailVerificationTokenEntity.builder()
                .user(user)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES))
                .build();
        tokenRepository.save(entity);
        emailSender.sendVerificationEmail(user.getEmail(), verificationBaseUrl + "?token=" + rawToken);
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        String tokenHash = sha256Hex(rawToken.trim());
        EmailVerificationTokenEntity token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token already used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token expired");
        }

        UserEntity user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    @Override
    @Transactional
    public void resend(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }

            List<EmailVerificationTokenEntity> activeTokens = tokenRepository.findAllByUserAndUsedAtIsNull(user);
            LocalDateTime now = LocalDateTime.now();
            for (EmailVerificationTokenEntity token : activeTokens) {
                token.setUsedAt(now);
            }
            tokenRepository.saveAll(activeTokens);

            createAndSendVerification(user);
        });
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

