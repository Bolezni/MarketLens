package com.bolezni.mvp_test.authorization.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void sendVerificationEmail(String to, String verificationLink) {
        log.info("Verification email to {}: {}", to, verificationLink);
    }
}

