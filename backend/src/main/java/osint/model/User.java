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

    @Column(name = "role")
    private String role; // Legacy single-role column

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled = false;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "user_file")
    private String userFile;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // Constructors
    public User() {
        // createdAt will be set by database default
    }

    public User(String email, String passwordHash, Set<Role> roles) {
        this();
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserFile() {
        return userFile;
    }

    public void setUserFile(String userFile) {
        this.userFile = userFile;
    }

    public Boolean getSmsMfaEnabled() {
        return mfaEnabled;
    }

    public void setSmsMfaEnabled(Boolean smsMfaEnabled) {
        this.mfaEnabled = smsMfaEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
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
        private String phoneNumber;
        private String userFile;
        private String totpSecret;
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

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder userFile(String userFile) {
            this.userFile = userFile;
            return this;
        }

        public Builder totpSecret(String totpSecret) {
            this.totpSecret = totpSecret;
            return this;
        }

        public User build() {
            User user = new User(email, passwordHash, roles);
            user.setFullName(fullName);
            user.setRole(role);
            user.setIsVerified(isVerified);
            user.setMfaEnabled(mfaEnabled);
            user.setPhoneNumber(phoneNumber);
            user.setUserFile(userFile);
            user.setTotpSecret(totpSecret);
            if (createdAt != null)
                user.setCreatedAt(createdAt);
            if (lastLogin != null)
                user.setLastLogin(lastLogin);
            return user;
        }
    }
}
