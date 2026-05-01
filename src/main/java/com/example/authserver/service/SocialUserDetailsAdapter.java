package com.example.authserver.service;

import com.example.authserver.entity.User;
import com.example.authserver.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Bridges our User entity with Spring's OAuth2User interface,
 * so that both social and local users work uniformly in Spring Security.
 */
public class SocialUserDetailsAdapter extends UserDetailsImpl implements OAuth2User {

    private final Map<String, Object> attributes;

    public SocialUserDetailsAdapter(User user, Map<String, Object> attributes) {
        super(user);
        this.attributes = attributes;
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName()                     { return getUsername(); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }
}
