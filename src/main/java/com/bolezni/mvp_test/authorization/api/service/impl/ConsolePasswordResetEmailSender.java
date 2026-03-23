package com.bolezni.mvp_test.authorization.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.type", havingValue = "console", matchIfMissing = true)
public class ConsolePasswordResetEmailSender implements PasswordResetEmailSender {

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("Password reset email to {}: {}", to, resetLink);
    }
}

