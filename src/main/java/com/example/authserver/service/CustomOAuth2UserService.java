package com.example.authserver.service;

import com.example.authserver.entity.User;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UserRepository;
import com.example.authserver.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles social login (Google, GitHub).
 * Provisions a local user on first login, or loads existing one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId;
        String email;
        String username;

        if ("google".equalsIgnoreCase(registrationId)) {
            providerId = oAuth2User.getAttribute("sub");
            email      = oAuth2User.getAttribute("email");
            username   = buildUsername(oAuth2User.getAttribute("name"), email);
        } else if ("github".equalsIgnoreCase(registrationId)) {
            providerId = String.valueOf(oAuth2User.<Integer>getAttribute("id"));
            email      = oAuth2User.getAttribute("email");
            String login = oAuth2User.getAttribute("login");
            username   = buildUsername(login, email);
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        if (email == null) {
            throw new OAuth2AuthenticationException(
                    "Email not provided by " + registrationId + ". Please grant email permission.");
        }

        User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                .orElseGet(() -> provisionNewUser(registrationId, providerId, email, username));

        log.debug("Social login: {} via {}", user.getEmail(), registrationId);
        return new SocialUserDetailsAdapter(user, oAuth2User.getAttributes());
    }

    private User provisionNewUser(String provider, String providerId, String email, String username) {
        // Check if email is already used by a local account
        if (userRepository.existsByEmail(email)) {
            log.warn("Social login collision: email {} already registered locally", email);
            throw new OAuth2AuthenticationException(
                    "An account with this email already exists. Please log in with your password.");
        }

        // Ensure username is unique
        String finalUsername = username;
        int suffix = 1;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + suffix++;
        }

        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider(provider)
                .providerId(providerId)
                .enabled(true)
                .build();

        roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);
        user = userRepository.save(user);
        log.info("Provisioned social user: {} via {}", email, provider);
        return user;
    }

    private String buildUsername(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.toLowerCase().replaceAll("[^a-z0-9._-]", "").substring(0, Math.min(name.length(), 40));
        }
        if (email != null && email.contains("@")) {
            return email.split("@")[0].replaceAll("[^a-z0-9._-]", "");
        }
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }
}
