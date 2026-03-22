package com.bolezni.mvp_test.authorization.api.service;

public interface EmailSender {
    void sendVerificationEmail(String to, String verificationLink);
}

