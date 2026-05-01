package com.example.authserver.controller;

import com.example.authserver.service.PasswordResetService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // ── Forgot password form ─────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam @Email @NotBlank String email,
                                RedirectAttributes redirectAttributes) {
        passwordResetService.initiatePasswordReset(email);
        // Always show success — never reveal whether email exists
        redirectAttributes.addFlashAttribute("message",
                "If that email is registered, you will receive a reset link shortly.");
        return "redirect:/forgot-password";
    }

    // ── Reset password form (via email link) ─────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("error", "This link is invalid or has expired.");
            return "auth/reset-password-invalid";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam @NotBlank @Size(min = 8) String password,
                                @RequestParam @NotBlank String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }

        boolean success = passwordResetService.validateAndResetPassword(token, password);
        if (!success) {
            redirectAttributes.addFlashAttribute("error", "Link expired or already used.");
            return "redirect:/reset-password?token=" + token;
        }

        redirectAttributes.addFlashAttribute("success", "Password changed! You can now log in.");
        return "redirect:/login";
    }
}
