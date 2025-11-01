package osint.dto;

public class MfaSetupResponse {
    private String totpSecret;
    private String totpUri;
    private String qrCodeUrl; // Optional, can be generated on frontend

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String totpSecret, String totpUri) {
        this.totpSecret = totpSecret;
        this.totpUri = totpUri;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public String getTotpUri() {
        return totpUri;
    }

    public void setTotpUri(String totpUri) {
        this.totpUri = totpUri;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
}

