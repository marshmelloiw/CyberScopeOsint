package osint.service;

import osint.dto.JwtResponse;
import osint.dto.MfaSetupResponse;
import osint.repository.PasswordResetTokenRepository;
import osint.repository.UserRepository;
import osint.model.PasswordResetToken;
import osint.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.binary.Base32;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // In-memory storage for TOTP secrets (not in DB schema)
    // In production, consider using Redis or another external storage
    private final Map<String, String> totpSecrets = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public void register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        String hash = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .email(email)
                .passwordHash(hash)
                .role("USER") // Default role: USER, ADMIN, or CORPORATE
                .isVerified(false)
                .mfaEnabled(false)
                .build();
        userRepository.save(user);
    }

    public JwtResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String mockJwt = "mock-" + UUID.randomUUID();
        return JwtResponse.builder()
                .token(mockJwt)
                .mfaRequired(false)
                .build();
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            Instant expires = Instant.now().plus(30, ChronoUnit.MINUTES);
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .token(token)
                    .email(email)
                    .expiresAt(expires)
                    .build());
            System.out.println("[RESET] email=" + email + " token=" + token + " expires=" + expires);
        });
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        User user = userRepository.findByEmail(prt.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found for token"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByToken(token);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

    public void setupSmsMfa(String email, String phoneNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // phoneNumber is not stored in DB schema, only enable MFA
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public boolean verifySmsMfa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA not enabled for user");
        }

        // Mock SMS verification - in production, verify against SMS service
        return "123456".equals(code);
    }

    public MfaSetupResponse setupTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate a random secret key (20 bytes = 160 bits, standard for TOTP)
        byte[] secretBytes = new byte[20];
        new java.security.SecureRandom().nextBytes(secretBytes);
        Base32 base32 = new Base32();
        String secret = base32.encodeToString(secretBytes);

        // Save secret in memory (not in DB schema)
        // In production, consider using Redis or another external storage
        totpSecrets.put(email, secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        // Generate TOTP URI for QR code
        // Format: otpauth://totp/Issuer:UserEmail?secret=SECRET&issuer=Issuer
        String issuer = "CyberScope";
        String totpUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, email, secret, issuer);

        return new MfaSetupResponse(secret, totpUri);
    }

    public boolean verifyTotpMfa(String email, String totpToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String secret = totpSecrets.get(email);
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("TOTP MFA not setup for user");
        }

        // For now, accept any 6-digit code as a mock
        // In production, use a proper TOTP library (e.g., com.warrenstrange:googleauth)
        // to verify the token against the secret
        if (totpToken == null || totpToken.length() != 6 || !totpToken.matches("\\d{6}")) {
            return false;
        }

        // Mock verification: accept any valid format
        // TODO: Replace with actual TOTP verification
        // TOTPVerifier verifier = new TOTPVerifier();
        // return verifier.verify(secret, totpToken);

        // For demo purposes, accept "123456" or any code if secret exists
        // In production, implement proper TOTP verification
        return true;
    }

    public void enableTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!totpSecrets.containsKey(email)) {
            throw new IllegalArgumentException("TOTP secret not found. Please setup TOTP first.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public void disableTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        totpSecrets.remove(email);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }
}