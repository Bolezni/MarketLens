package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.AuthRequest;
import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.security.CustomUserDetails;
import com.bolezni.mvp_test.authorization.api.security.jwt.JwtProvider;
import com.bolezni.mvp_test.authorization.api.service.AuthorizationService;
import com.bolezni.mvp_test.authorization.store.RefreshTokenEntity;
import com.bolezni.mvp_test.authorization.store.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
public class BaseAuthorizationService implements AuthorizationService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AuthResponse authorize(AuthRequest authRequest) {
        String email = authRequest.email().trim().toLowerCase(Locale.ROOT);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, authRequest.password())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String jwt = jwtProvider.buildToken(userDetails);
            String refreshToken = jwtProvider.buildRefreshToken(userDetails);

            RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                    .user(userDetails.userEntity())
                    .tokenHash(sha256Hex(refreshToken))
                    .expiresAt(LocalDateTime.ofInstant(jwtProvider.extractExpiration(refreshToken).toInstant(), ZoneId.systemDefault()))
                    .build();

            refreshTokenRepository.save(refreshTokenEntity);

            return new AuthResponse(jwt, "Bearer", refreshToken);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e);
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email is not verified", e);
        }
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
