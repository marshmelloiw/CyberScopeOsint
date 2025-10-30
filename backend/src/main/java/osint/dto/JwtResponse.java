package osint.dto;

public class JwtResponse {
    private String token;
    private boolean mfaRequired;

    // Constructors
    public JwtResponse() {
    }

    public JwtResponse(String token, boolean mfaRequired) {
        this.token = token;
        this.mfaRequired = mfaRequired;
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

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private boolean mfaRequired;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public JwtResponse build() {
            return new JwtResponse(token, mfaRequired);
        }
    }
}
