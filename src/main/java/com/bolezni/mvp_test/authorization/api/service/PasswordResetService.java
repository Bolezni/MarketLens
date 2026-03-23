package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.ForgotPasswordRequest;
import com.bolezni.mvp_test.authorization.api.dto.ResetPasswordRequest;
import com.bolezni.mvp_test.authorization.api.dto.MessageResponse;

public interface PasswordResetService {
    MessageResponse forgot(ForgotPasswordRequest request);

    MessageResponse reset(ResetPasswordRequest request);
}

