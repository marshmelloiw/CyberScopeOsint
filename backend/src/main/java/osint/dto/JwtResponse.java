package osint.dto;

public class JwtResponse {
    private String token;
    private String token_type; // 🆕 Eklendi
    private boolean mfaRequired;

    // Constructors
    public JwtResponse() {
    }

    public JwtResponse(String token, String token_type, boolean mfaRequired) {
        this.token = token;
        this.token_type = token_type;
        this.mfaRequired = mfaRequired;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String token_type = "Bearer"; // 🆕 Varsayılan
        private boolean mfaRequired;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder token_type(String token_type) {
            this.token_type = token_type;
            return this;
        }

        public Builder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public JwtResponse build() {
            return new JwtResponse(token, token_type, mfaRequired);
        }
    }
}
