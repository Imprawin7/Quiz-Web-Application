package com.Quizvera.service;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.SendEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {

        Resend resend = new Resend(resendApiKey);

        String htmlContent =
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto;\">" +
                "<h2 style=\"color: #4f46e5;\">Quizvera Password Reset</h2>" +

                "<p>Hi " + fullName + ",</p>" +

                "<p>We received a request to reset your Quizvera password.</p>" +

                "<p>Click the button below to choose a new password:</p>" +

                "<p>" +
                "<a href=\"" + resetLink + "\" " +
                "style=\"display:inline-block;padding:12px 24px;" +
                "background:#4f46e5;color:white;text-decoration:none;" +
                "border-radius:6px;\">" +
                "Reset Password" +
                "</a>" +
                "</p>" +

                "<p>Or copy and paste this link into your browser:</p>" +

                "<p>" + resetLink + "</p>" +

                "<p><strong>This link expires in 30 minutes.</strong></p>" +

                "<p>If you didn't request this password reset, you can safely ignore this email.</p>" +

                "<p>— Quizvera</p>" +

                "</div>";

        SendEmailRequest request = SendEmailRequest.builder()
                .from("Quizvera <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Reset your Quizvera password")
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(request);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}