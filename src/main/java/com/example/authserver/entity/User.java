package com.example.authserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.NumericBooleanConverter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "ACCOUNT_NON_EXPIRED", nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(name = "ACCOUNT_NON_LOCKED", nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(name = "CREDENTIALS_NON_EXPIRED", nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    @Builder.Default
    private boolean credentialsNonExpired = true;

    @Column(name = "MFA_ENABLED", nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "MFA_SECRET", length = 64)
    private String mfaSecret;

    // Social login: "local", "google", "github"
    @Column(name = "PROVIDER", length = 20)
    @Builder.Default
    private String provider = "local";

    @Column(name = "PROVIDER_ID", length = 255)
    private String providerId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "USER_ROLES",
        joinColumns = @JoinColumn(name = "USER_ID"),
        inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean isLocalUser() {
        return "local".equals(this.provider);
    }
}
