package com.example.authserver.config;

import com.example.authserver.security.UserDetailsImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * After successful username/password login, checks whether the user has MFA enabled.
 * If yes → stores auth in session and redirects to /mfa/verify.
 * If no  → proceeds to the original requested URL (standard saved-request behaviour).
 */
public class MfaAwareAuthenticationSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String MFA_REQUIRED_KEY = "MFA_REQUIRED";
    public static final String PRE_MFA_AUTH_KEY  = "PRE_MFA_AUTH";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            if (userDetails.getUser().isMfaEnabled()) {
                HttpSession session = request.getSession();
                session.setAttribute(MFA_REQUIRED_KEY, true);
                session.setAttribute(PRE_MFA_AUTH_KEY, authentication);
                // Clear the security context — user is NOT yet fully authenticated
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/mfa/verify");
                return;
            }
        }
        // No MFA — normal flow
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
