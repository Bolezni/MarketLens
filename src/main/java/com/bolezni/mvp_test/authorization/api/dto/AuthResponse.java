package com.bolezni.mvp_test.authorization.api.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String refreshToken
) {
}
