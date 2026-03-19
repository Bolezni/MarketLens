package com.bolezni.mvp_test.authorization.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
