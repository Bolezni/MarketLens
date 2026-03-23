package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.service.PasswordResetEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.type", havingValue = "smtp")
@RequiredArgsConstructor
public class JavaMailPasswordResetEmailSender implements PasswordResetEmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    @Async("mailTaskExecutor")
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password reset");
        message.setText("To reset your password, follow this link:\n" + resetLink);

        javaMailSender.send(message);
        log.info("Password reset email sent to {}", to);
    }
}

