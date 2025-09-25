package osint.controller;

import osint.dto.*;
import osint.util.TOTPUtil;
import osint.util.TOTPVerifier;
import osint.service.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SECRET_KEY = "mySecretKey123456789012345678901234567890";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // Basit in-memory kullanıcı durumu (demo amaçlı)
    private static final Map<String, UserMfaState> userState = new HashMap<>();
    private static final Map<String, SmsCode> smsCodes = new HashMap<>();
    private final SmsService smsService = new SmsService();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("user", Map.of(
                "id", 1,
                "name", request.getFirstName() + " " + request.getLastName(),
                "email", request.getEmail()));
        userState.putIfAbsent(request.getEmail(), new UserMfaState());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Demo: tek kullanıcı
        boolean valid = "admin@example.com".equals(request.getUsername()) && "admin123".equals(request.getPassword());
        if (!valid) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid credentials"));
        }

        UserMfaState state = userState.computeIfAbsent(request.getUsername(), k -> new UserMfaState());
        // SMS MFA öncelikli kontrol
        if (state.smsMfaEnabled && state.phoneNumber != null) {
            String code = String.format("%06d", (int) (Math.random() * 1_000_000));
            long expiry = System.currentTimeMillis() + 5 * 60 * 1000; // 5 dk
            smsCodes.put(request.getUsername(), new SmsCode(code, expiry));
            smsService.sendCode(state.phoneNumber, code);

            Map<String, Object> resp = new HashMap<>();
            resp.put("smsRequired", true);
            resp.put("user", Map.of(
                    "id", 1,
                    "name", "Admin User",
                    "email", request.getUsername()));
            return ResponseEntity.ok(resp);
        }
        if (state.mfaEnabled) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("mfaRequired", true);
            resp.put("user", Map.of(
                    "id", 1,
                    "name", "Admin User",
                    "email", request.getUsername()));
            return ResponseEntity.ok(resp);
        }

        String token = generateToken(request.getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("user", Map.of(
                "id", 1,
                "name", "Admin User",
                "email", request.getUsername()));
        return ResponseEntity.ok(response);
    }

    // SMS doğrulama endpointi
    @PostMapping("/sms/verify")
    public ResponseEntity<?> verifySms(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("username");
        String code = (String) body.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing username or code"));
        }

        SmsCode stored = smsCodes.get(email);
        if (stored == null || System.currentTimeMillis() > stored.expiry) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Code expired"));
        }
        if (!stored.code.equals(code)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid code"));
        }

        smsCodes.remove(email);
        String token = generateToken(email);
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("user", Map.of(
                "id", 1,
                "name", "Admin User",
                "email", email));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse("Password reset email sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    // MFA: Setup - gizli anahtar üret ve otpauth URI dön
    @PostMapping("/mfa/setup")
    public ResponseEntity<?> mfaSetup(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> body) {
        String email = extractEmailFromAuth(authorization);
        if (email == null && body != null) {
            Object u = body.get("username");
            if (u instanceof String)
                email = (String) u;
        }
        if (email == null)
            email = "admin@example.com"; // demo fallback

        UserMfaState state = userState.computeIfAbsent(email, k -> new UserMfaState());
        if (state.mfaSecret == null || state.mfaSecret.isEmpty()) {
            state.mfaSecret = TOTPUtil.generateSecret();
        }
        String uri = TOTPUtil.buildOtpAuthUrl("CyberScope", email, state.mfaSecret);

        Map<String, Object> resp = new HashMap<>();
        resp.put("totp_secret", state.mfaSecret);
        resp.put("totp_uri", uri);
        resp.put("message", "QR kodu authenticator app ile tarayın");
        return ResponseEntity.ok(resp);
    }

    // MFA: Verify & Enable - 6 haneli kodu doğrula ve mfaEnabled=true yap
    @PostMapping("/mfa/verify")
    public ResponseEntity<?> mfaVerify(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("username", "admin@example.com");
        String codeStr = (String) body.get("totp_token");
        if (codeStr == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing TOTP token"));
        }
        int code;
        try {
            code = Integer.parseInt(codeStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid TOTP token"));
        }

        UserMfaState state = userState.computeIfAbsent(email, k -> new UserMfaState());
        if (state.mfaSecret == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("MFA not initialized"));
        }
        try {
            TOTPVerifier verifier = new TOTPVerifier();
            if (verifier.verify(state.mfaSecret, code)) {
                state.mfaEnabled = true;
                return ResponseEntity.ok(Map.of("enabled", true));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Verification error"));
        }
    }

    // MFA: Authenticate - doğru kod ise JWT ver
    @PostMapping("/mfa/authenticate")
    public ResponseEntity<?> mfaAuthenticate(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("username", "admin@example.com");
        String codeStr = (String) body.get("totp_token");
        if (codeStr == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing TOTP token"));
        }
        int code;
        try {
            code = Integer.parseInt(codeStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid TOTP token"));
        }

        UserMfaState state = userState.computeIfAbsent(email, k -> new UserMfaState());
        if (!state.mfaEnabled || state.mfaSecret == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("MFA not enabled"));
        }
        try {
            TOTPVerifier verifier = new TOTPVerifier();
            if (verifier.verify(state.mfaSecret, code)) {
                String token = generateToken(email);
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("type", "Bearer");
                response.put("user", Map.of(
                        "id", 1,
                        "name", "Admin User",
                        "email", email));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Verification error"));
        }
    }

    // MFA: Disable TOTP
    @PostMapping("/mfa/disable")
    public ResponseEntity<?> mfaDisable(@RequestBody Map<String, Object> body) {
        String email = (String) body.getOrDefault("username", "admin@example.com");
        UserMfaState state = userState.computeIfAbsent(email, k -> new UserMfaState());
        state.mfaEnabled = false;
        state.mfaSecret = null;
        return ResponseEntity.ok(Map.of("enabled", false));
    }

    // Settings: SMS MFA enable/disable ve telefon numarası kaydetme
    @PostMapping("/mfa/sms/setup")
    public ResponseEntity<?> setupSmsMfa(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("username");
        String phone = (String) body.get("phoneNumber");
        Boolean enabled = (Boolean) body.get("enabled");
        if (email == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing username"));
        }
        UserMfaState state = userState.computeIfAbsent(email, k -> new UserMfaState());
        if (phone != null) {
            state.phoneNumber = phone;
        }
        if (enabled != null) {
            state.smsMfaEnabled = enabled;
        }
        return ResponseEntity.ok(Map.of(
                "smsMfaEnabled", state.smsMfaEnabled,
                "phoneNumber", state.phoneNumber));
    }

    private String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String extractEmailFromAuth(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer "))
                return null;
            String token = authorization.substring("Bearer ".length());
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private static class UserMfaState {
        boolean mfaEnabled = false;
        String mfaSecret;
        boolean smsMfaEnabled = false; // varsayılan kapalı
        String phoneNumber; // kullanıcı ayarlarda ekler
    }

    private static class SmsCode {
        String code;
        long expiry;

        SmsCode(String code, long expiry) {
            this.code = code;
            this.expiry = expiry;
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}