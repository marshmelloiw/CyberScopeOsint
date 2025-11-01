package osint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MfaVerifyRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "TOTP token is required")
    @Size(min = 6, max = 6, message = "TOTP token must be 6 digits")
    private String totpToken;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String username, String totpToken) {
        this.username = username;
        this.totpToken = totpToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTotpToken() {
        return totpToken;
    }

    public void setTotpToken(String totpToken) {
        this.totpToken = totpToken;
    }
}

