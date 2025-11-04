package osint.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@jakarta.persistence.Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "role", length = 50)
    private String role; // ADMIN, USER, CORPORATE

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled = false;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // For backward compatibility - keep roles but map to role field
    @Transient
    private Set<Role> roles = new HashSet<>();

    // Constructors
    public User() {
        // createdAt will be set by database default
    }

    public User(Long id, String email, String passwordHash, Set<Role> roles) {
        this();
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles != null ? roles : new HashSet<>();
        this.mfaEnabled = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Backward compatibility methods
    public String getPhoneNumber() {
        return null; // Not in DB schema
    }

    public void setPhoneNumber(String phoneNumber) {
        // Not in DB schema
    }

    public Boolean getSmsMfaEnabled() {
        return mfaEnabled;
    }

    public void setSmsMfaEnabled(Boolean smsMfaEnabled) {
        this.mfaEnabled = smsMfaEnabled;
    }

    public String getTotpSecret() {
        return null; // Not in DB schema
    }

    public void setTotpSecret(String totpSecret) {
        // Not in DB schema
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String email;
        private String passwordHash;
        private String fullName;
        private String role;
        private Boolean isVerified = false;
        private Boolean mfaEnabled = false;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private Set<Role> roles = new HashSet<>();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            this.roles = roles != null ? roles : new HashSet<>();
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder isVerified(Boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }

        public Builder mfaEnabled(Boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }

        public User build() {
            User user = new User(id, email, passwordHash, roles);
            user.setFullName(fullName);
            user.setRole(role);
            user.setIsVerified(isVerified);
            user.setMfaEnabled(mfaEnabled);
            if (createdAt != null) user.setCreatedAt(createdAt);
            if (lastLogin != null) user.setLastLogin(lastLogin);
            return user;
        }
    }
}
