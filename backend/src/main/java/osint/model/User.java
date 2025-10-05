package osint.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "sms_mfa_enabled")
    private Boolean smsMfaEnabled = false;

    @Column(name = "totp_secret")
    private String totpSecret;

    // Constructors
    public User() {
    }

    public User(Long id, String email, String passwordHash, Set<Role> roles) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles != null ? roles : new HashSet<>();
        this.smsMfaEnabled = false;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getSmsMfaEnabled() {
        return smsMfaEnabled;
    }

    public void setSmsMfaEnabled(Boolean smsMfaEnabled) {
        this.smsMfaEnabled = smsMfaEnabled;
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
        private Set<Role> roles = new HashSet<>();
        private String phoneNumber;
        private Boolean smsMfaEnabled = false;
        private String totpSecret;

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

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder smsMfaEnabled(Boolean smsMfaEnabled) {
            this.smsMfaEnabled = smsMfaEnabled;
            return this;
        }

        public Builder totpSecret(String totpSecret) {
            this.totpSecret = totpSecret;
            return this;
        }

        public User build() {
            User user = new User(id, email, passwordHash, roles);
            user.setPhoneNumber(phoneNumber);
            user.setSmsMfaEnabled(smsMfaEnabled);
            user.setTotpSecret(totpSecret);
            return user;
        }
    }
}
