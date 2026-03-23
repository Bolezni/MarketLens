package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.type", havingValue = "smtp")
public class DefaultEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String to, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirm email");
        message.setText("Password reset email sent to:\n" + verificationLink);

        javaMailSender.send(message);
        log.info("Email to confirm the mail sent to the address {}", to);
    }
}
