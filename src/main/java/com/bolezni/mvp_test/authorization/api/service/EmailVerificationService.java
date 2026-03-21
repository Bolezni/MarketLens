package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.store.UserEntity;

public interface EmailVerificationService {
    void createAndSendVerification(UserEntity user);

    void verify(String rawToken);

    void resend(String email);
}

