package com.bolezni.mvp_test.authorization.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String password,
        @NotBlank String passwordConfirmation
) {
}

