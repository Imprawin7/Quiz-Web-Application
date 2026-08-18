package com.Quizvera.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your Quizvera password");
        message.setText(
                "Hi " + fullName + ",\n\n" +
                "We received a request to reset your Quizvera password. " +
                "Click the link below to choose a new one:\n\n" +
                resetLink + "\n\n" +
                "This link expires in 30 minutes. If you didn't request this, you can safely ignore this email " +
                "— your password will remain unchanged.\n\n" +
                "— Quizvera"
        );
        mailSender.send(message);
    }
}