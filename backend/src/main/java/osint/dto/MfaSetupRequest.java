package osint.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaSetupRequest {
    @NotBlank(message = "Username is required")
    private String username;

    public MfaSetupRequest() {
    }

    public MfaSetupRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

