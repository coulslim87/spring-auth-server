package com.example.authserver.controller;

import com.example.authserver.config.MfaAwareAuthenticationSuccessHandler;
import com.example.authserver.security.UserDetailsImpl;
import com.example.authserver.service.MfaService;
import com.example.authserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;
    private final UserService userService;

    // ── Setup MFA (from account settings) ──────────────────────

    @GetMapping("/setup")
    public String setupPage(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        String secret = mfaService.generateSecret();
        String otpUri = mfaService.getOtpAuthUri(secret, principal.getUsername());
        String qrCode = mfaService.generateQrCodeBase64(otpUri);

        model.addAttribute("secret", secret);
        model.addAttribute("qrCode", qrCode);
        return "mfa/setup";
    }

    @PostMapping("/setup/confirm")
    public String confirmSetup(@AuthenticationPrincipal UserDetailsImpl principal,
                               @RequestParam String secret,
                               @RequestParam String code,
                               RedirectAttributes redirectAttributes) {
        if (!mfaService.verifyCode(secret, code)) {
            redirectAttributes.addFlashAttribute("error", "Invalid code. Please try again.");
            return "redirect:/mfa/setup";
        }

        userService.enableMfa(principal.getUser().getId(), secret);
        redirectAttributes.addFlashAttribute("success", "Two-factor authentication enabled!");
        return "redirect:/account";
    }

    @PostMapping("/disable")
    public String disableMfa(@AuthenticationPrincipal UserDetailsImpl principal,
                             @RequestParam String code,
                             RedirectAttributes redirectAttributes) {
        if (!mfaService.verifyCode(principal.getUser().getMfaSecret(), code)) {
            redirectAttributes.addFlashAttribute("error", "Invalid code.");
            return "redirect:/account";
        }

        userService.disableMfa(principal.getUser().getId());
        redirectAttributes.addFlashAttribute("success", "Two-factor authentication disabled.");
        return "redirect:/account";
    }

    // ── Verify MFA after login ──────────────────────────────────

    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        if (session.getAttribute(MfaAwareAuthenticationSuccessHandler.MFA_REQUIRED_KEY) == null) {
            return "redirect:/login";
        }
        return "mfa/verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String code,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(MfaAwareAuthenticationSuccessHandler.MFA_REQUIRED_KEY) == null) {
            return "redirect:/login";
        }

        Authentication preAuth = (Authentication) session.getAttribute(
                MfaAwareAuthenticationSuccessHandler.PRE_MFA_AUTH_KEY);

        if (preAuth == null) {
            return "redirect:/login";
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) preAuth.getPrincipal();
        if (!mfaService.verifyCode(userDetails.getUser().getMfaSecret(), code)) {
            redirectAttributes.addFlashAttribute("error", "Invalid code. Please try again.");
            return "redirect:/mfa/verify";
        }

        // Complete authentication — set security context
        UsernamePasswordAuthenticationToken fullyAuthenticated =
                UsernamePasswordAuthenticationToken.authenticated(
                        preAuth.getPrincipal(),
                        preAuth.getCredentials(),
                        preAuth.getAuthorities()
                );

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(fullyAuthenticated);
        SecurityContextHolder.setContext(ctx);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        session.removeAttribute(MfaAwareAuthenticationSuccessHandler.MFA_REQUIRED_KEY);
        session.removeAttribute(MfaAwareAuthenticationSuccessHandler.PRE_MFA_AUTH_KEY);

        log.info("MFA verification successful for: {}", userDetails.getUsername());
        return "redirect:/";
    }
}
