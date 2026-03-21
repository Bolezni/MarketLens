package com.bolezni.mvp_test.authorization.api.service.impl;

public interface EmailSender {
    void sendVerificationEmail(String to, String verificationLink);
}

