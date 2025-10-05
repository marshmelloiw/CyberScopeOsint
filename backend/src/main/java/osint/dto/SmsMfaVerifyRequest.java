package osint.dto;

import jakarta.validation.constraints.NotBlank;

public class SmsMfaVerifyRequest {
    @NotBlank(message = "SMS code is required")
    private String code;

    public SmsMfaVerifyRequest() {
    }

    public SmsMfaVerifyRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
