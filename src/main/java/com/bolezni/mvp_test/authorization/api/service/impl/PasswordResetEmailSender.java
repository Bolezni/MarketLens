package com.bolezni.mvp_test.authorization.api.service.impl;

public interface PasswordResetEmailSender {
    void sendPasswordResetEmail(String to, String resetLink);
}

