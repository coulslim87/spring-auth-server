package com.example.authserver.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
@Slf4j
public class AdminClientController {

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String listClients(Model model) {
        // JdbcRegisteredClientRepository doesn't expose findAll — we'd need a custom query
        // For simplicity, we redirect to the new client form (extend as needed)
        model.addAttribute("info", "Use the form below to register new OAuth2 clients.");
        return "admin/clients/list";
    }

    @GetMapping("/new")
    public String newClientForm(Model model) {
        return "admin/clients/form";
    }

    @PostMapping("/new")
    public String createClient(@RequestParam String clientId,
                               @RequestParam String clientSecret,
                               @RequestParam String clientName,
                               @RequestParam String redirectUris,
                               @RequestParam String scopes,
                               @RequestParam(defaultValue = "false") boolean requireConsent,
                               @RequestParam(defaultValue = "1") int accessTokenHours,
                               @RequestParam(defaultValue = "30") int refreshTokenDays,
                               RedirectAttributes redirectAttributes) {
        try {
            if (clientRepository.findByClientId(clientId) != null) {
                redirectAttributes.addFlashAttribute("error", "Client ID already exists: " + clientId);
                return "redirect:/admin/clients/new";
            }

            var builder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientName(clientName)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(requireConsent)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(accessTokenHours))
                            .refreshTokenTimeToLive(Duration.ofDays(refreshTokenDays))
                            .reuseRefreshTokens(false)
                            .build());

            // Always add openid
            builder.scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL);
            Arrays.stream(scopes.split("[,\\s]+"))
                  .map(String::trim)
                  .filter(s -> !s.isBlank())
                  .forEach(builder::scope);

            Arrays.stream(redirectUris.split("[,\\n]+"))
                  .map(String::trim)
                  .filter(u -> !u.isBlank())
                  .forEach(builder::redirectUri);

            clientRepository.save(builder.build());
            redirectAttributes.addFlashAttribute("success", "Client registered: " + clientId);
            log.info("Registered OAuth2 client: {}", clientId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/clients";
    }
}
