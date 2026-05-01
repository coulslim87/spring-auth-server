package com.example.authserver.service;

import com.example.authserver.dto.UserDto;
import com.example.authserver.dto.UserForm;
import com.example.authserver.entity.Role;
import com.example.authserver.entity.User;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> search(String query, Pageable pageable) {
        return userRepository.search(query, pageable).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public User createUser(UserForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + form.getUsername());
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + form.getEmail());
        }
        if (!StringUtils.hasText(form.getPassword())) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }

        User user = User.builder()
                .username(form.getUsername())
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .enabled(form.isEnabled())
                .provider("local")
                .build();

        applyRoles(user, form.getRoles());
        user = userRepository.save(user);
        log.info("Created user: {}", user.getUsername());
        return user;
    }

    @Transactional
    public User updateUser(Long id, UserForm form) {
        User user = findById(id);

        // Check uniqueness if changed
        if (!user.getUsername().equals(form.getUsername()) &&
            userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + form.getUsername());
        }
        if (!user.getEmail().equals(form.getEmail()) &&
            userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + form.getEmail());
        }

        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setEnabled(form.isEnabled());

        if (StringUtils.hasText(form.getPassword())) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        if (form.isResetMfa()) {
            user.setMfaEnabled(false);
            user.setMfaSecret(null);
        }

        applyRoles(user, form.getRoles());
        log.info("Updated user: {}", user.getUsername());
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void enableMfa(Long userId, String secret) {
        User user = findById(userId);
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);
    }

    @Transactional
    public void disableMfa(Long userId) {
        User user = findById(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    private void applyRoles(User user, Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            // Default role for new users
            roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);
            return;
        }
        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name)))
                .collect(Collectors.toSet());
        user.getRoles().clear();
        user.getRoles().addAll(roles);
    }
}
