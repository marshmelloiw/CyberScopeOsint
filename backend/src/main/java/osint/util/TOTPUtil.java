package osint.util;

import org.apache.commons.codec.binary.Base32;

import java.security.SecureRandom;

public class TOTPUtil {
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes).replace("=", "");
    }

    public static String buildOtpAuthUrl(String appName, String userEmail, String base32Secret) {
        String issuer = urlEncode(appName);
        String label = urlEncode(appName + ":" + userEmail);
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s&digits=6&period=30", label, base32Secret, issuer);
    }

    private static String urlEncode(String input) {
        return input.replace(" ", "%20");
    }
}
