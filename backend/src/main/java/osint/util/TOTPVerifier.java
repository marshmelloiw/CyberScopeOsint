package osint.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

public class TOTPVerifier {
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;

    public boolean verify(String base32Secret, int code) throws Exception {
        long epochSeconds = System.currentTimeMillis() / 1000L;
        long currentCounter = epochSeconds / TIME_STEP_SECONDS;
        int current = generateCode(base32Secret, currentCounter);
        int previous = generateCode(base32Secret, currentCounter - 1);
        int next = generateCode(base32Secret, currentCounter + 1);
        return code == current || code == previous || code == next;
    }

    private int generateCode(String base32Secret, long counter) throws Exception {
        byte[] key = new Base32().decode(base32Secret);
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(counterBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);
        int otp = binary % (int) Math.pow(10, DIGITS);
        return otp;
    }
}
