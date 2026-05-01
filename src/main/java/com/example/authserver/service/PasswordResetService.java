package com.example.authserver.service;

import com.example.authserver.entity.PasswordResetToken;
import com.example.authserver.entity.User;
import com.example.authserver.repository.PasswordResetTokenRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${app.password-reset.token-expiry-hours:24}")
    private int tokenExpiryHours;

    /**
     * Initiates the password reset flow. Always returns success to avoid user enumeration.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for unknown email: {}", email);
            return; // Silent — don't reveal whether email exists
        }

        User user = userOpt.get();
        if (!user.isLocalUser()) {
            log.debug("Password reset skipped for social user: {}", user.getEmail());
            return; // Social users don't have a local password
        }

        // Invalidate any existing tokens for this user by marking used
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .forEach(t -> t.setUsed(true));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(tokenExpiryHours))
                .build();

        tokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
        log.info("Password reset initiated for: {}", user.getEmail());
    }

    @Transactional
    public boolean validateAndResetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            log.warn("Password reset: token not found");
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (!resetToken.isValid()) {
            log.warn("Password reset: token expired or already used");
            return false;
        }

        User user = resetToken.getUser();
        userService.changePassword(user.getId(), newPassword);
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());
        log.info("Password reset successful for: {}", user.getEmail());
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isValidToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    /** Cleanup job — runs every day at 2 AM */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
        log.info("Cleaned up expired/used password reset tokens");
    }
}
