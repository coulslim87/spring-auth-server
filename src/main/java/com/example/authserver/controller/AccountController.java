package com.example.authserver.controller;

import com.example.authserver.security.UserDetailsImpl;
import com.example.authserver.service.EmailService;
import com.example.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String accountPage(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "account/index";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetailsImpl principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!passwordEncoder.matches(currentPassword, principal.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/account";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/account";
        }
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
            return "redirect:/account";
        }

        userService.changePassword(principal.getUser().getId(), newPassword);
        emailService.sendPasswordChangedEmail(principal.getUser().getEmail(), principal.getUsername());
        redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        return "redirect:/account";
    }
}
