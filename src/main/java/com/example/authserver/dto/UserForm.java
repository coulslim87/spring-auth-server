package com.example.authserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserForm {

    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, digits, '.', '_', '-'")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    // Only required when creating; blank = no change when updating
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
    private String password;

    private boolean enabled = true;

    private Set<String> roles;

    // True = admin is forcing MFA disable (e.g. user lost their device)
    private boolean resetMfa = false;

    public boolean isNew() {
        return id == null;
    }
}
