package osint.dto;

public class JwtResponse {
    private String token;
    private boolean mfaRequired;
    private String email;
    private java.util.List<String> roles;

    // Constructors
    public JwtResponse() {
    }

    public JwtResponse(String token, boolean mfaRequired, String email, java.util.List<String> roles) {
        this.token = token;
        this.mfaRequired = mfaRequired;
        this.email = email;
        this.roles = roles;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public java.util.List<String> getRoles() {
        return roles;
    }

    public void setRoles(java.util.List<String> roles) {
        this.roles = roles;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private boolean mfaRequired;
        private String email;
        private java.util.List<String> roles;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder roles(java.util.List<String> roles) {
            this.roles = roles;
            return this;
        }

        public JwtResponse build() {
            return new JwtResponse(token, mfaRequired, email, roles);
        }
    }
}
