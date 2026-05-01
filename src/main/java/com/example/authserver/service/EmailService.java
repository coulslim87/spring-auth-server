package com.example.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.from-name:Auth Server}")
    private String fromName;

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = String.format("""
                <html><body>
                <p>Hello <strong>%s</strong>,</p>
                <p>You requested a password reset. Click the button below to set a new password.</p>
                <p>This link expires in 24 hours.</p>
                <p>
                  <a href="%s" style="background:#4F46E5;color:white;padding:12px 24px;
                     border-radius:6px;text-decoration:none;font-weight:bold;">
                    Reset Password
                  </a>
                </p>
                <p>If you did not request this, you can safely ignore this email.</p>
                <p>— %s</p>
                </body></html>
                """, username, resetLink, fromName);

        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to " + fromName;
        String body = String.format("""
                <html><body>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your account has been created successfully.</p>
                <p>You can log in at: <a href="%s/login">%s/login</a></p>
                <p>— %s</p>
                </body></html>
                """, username, baseUrl, baseUrl, fromName);

        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordChangedEmail(String toEmail, String username) {
        String subject = "Your password has been changed";
        String body = String.format("""
                <html><body>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your password was successfully changed.</p>
                <p>If you did not make this change, please contact support immediately.</p>
                <p>— %s</p>
                </body></html>
                """, username, fromName);

        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't rethrow — email failure should not break the user flow
        }
    }
}
