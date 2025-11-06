package osint.dto;

public class JwtResponse {
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private boolean mfaRequired;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private boolean verified;
    private boolean mfaEnabled;
    private long expiresIn;

    public JwtResponse() {
    }

    private JwtResponse(Builder builder) {
        this.token = builder.token;
        this.refreshToken = builder.refreshToken;
        this.tokenType = builder.tokenType;
        this.mfaRequired = builder.mfaRequired;
        this.userId = builder.userId;
        this.email = builder.email;
        this.fullName = builder.fullName;
        this.role = builder.role;
        this.verified = builder.verified;
        this.mfaEnabled = builder.mfaEnabled;
        this.expiresIn = builder.expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String refreshToken;
        private String tokenType = "Bearer";
        private boolean mfaRequired;
        private Long userId;
        private String email;
        private String fullName;
        private String role;
        private boolean verified;
        private boolean mfaEnabled;
        private long expiresIn;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
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

        public Builder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public Builder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public JwtResponse build() {
            return new JwtResponse(this);
        }
    }
}
