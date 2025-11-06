package osint.service;

import osint.dto.JwtResponse;
import osint.dto.MfaSetupResponse;
import osint.repository.PasswordResetTokenRepository;
import osint.repository.RefreshTokenRepository;
import osint.repository.RoleRepository;
import osint.repository.UserRepository;
import osint.model.PasswordResetToken;
import osint.model.RefreshToken;
import osint.model.Role;
import osint.model.User;
import osint.util.JwtUtil;
import osint.util.TOTPVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.codec.binary.Base32;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;
    private final long refreshTokenValiditySeconds;
    private final TOTPVerifier totpVerifier = new TOTPVerifier();
    private static final Set<String> ALLOWED_ROLES = Set.of("admin", "analyst", "viewer");
    private static final String DEFAULT_ROLE = "viewer";

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.secret:change-me-change-me-change-me-123456}") String jwtSecret,
            @Value("${security.jwt.expiration:3600}") long jwtExpirationSeconds,
            @Value("${security.jwt.refresh-expiration:604800}") long refreshTokenValiditySeconds) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = new JwtUtil(jwtSecret, jwtExpirationSeconds);
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public Map<String, Object> register(String name, String email, String password, MultipartFile file) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email zorunludur");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Şifre zorunludur");
        }

        if (password.trim().length() < 8) {
            throw new IllegalArgumentException("Şifre en az 8 karakter olmalıdır");
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya seçilmedi");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Sadece PDF dosyası yüklenebilir");
        }

        // Save file
        String filePath;
        try {
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + "/uploads/user-files";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePathObj = Paths.get(uploadDir, uniqueFilename);
            Files.copy(file.getInputStream(), filePathObj, StandardCopyOption.REPLACE_EXISTING);
            filePath = uploadDir + "/" + uniqueFilename;
        } catch (IOException e) {
            throw new IllegalArgumentException("Dosya yüklenirken hata oluştu: " + e.getMessage());
        }

        String hash = passwordEncoder.encode(password.trim());

        // Create user
        User user = User.builder()
                .email(normalizedEmail)
                .fullName(name)
                .passwordHash(hash)
                .userFile(filePath)
                .role(DEFAULT_ROLE)
                .isVerified(false) // Requires admin approval
                .mfaEnabled(false)
                .build();
        assignRole(user, DEFAULT_ROLE);
        userRepository.save(user);

        // Return success message (no token - user needs admin approval)
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Kayıt başarılı. Hesabınızın aktifleştirilmesi için yönetici onayı gerekmektedir.");
        response.put("user_id", user.getId());

        return response;
    }

    public JwtResponse login(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Geçersiz kullanıcı adı veya şifre");
        }

        String normalizedEmail = email.trim().toLowerCase();

        logger.debug("Attempting login for email={}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz kullanıcı adı veya şifre"));
        String sanitizedPassword = rawPassword != null ? rawPassword.trim() : "";

        if (!passwordEncoder.matches(sanitizedPassword, user.getPasswordHash())) {
            logger.warn("Password mismatch for user id={} email={}", user.getId(), normalizedEmail);
            throw new IllegalArgumentException("Geçersiz kullanıcı adı veya şifre");
        }

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            logger.warn("Login blocked for unverified user id={} email={}", user.getId(), normalizedEmail);
            throw new IllegalArgumentException("Hesabınız aktif değil. Lütfen yöneticinizle iletişime geçin.");
        }

        boolean totpEnabled = Boolean.TRUE.equals(user.getMfaEnabled()) && user.getTotpSecret() != null;
        if (totpEnabled) {
            return JwtResponse.builder()
                    .mfaRequired(true)
                    .tokenType("mfa_required")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .verified(Boolean.TRUE.equals(user.getIsVerified()))
                    .mfaEnabled(true)
                    .expiresIn(jwtUtil.getExpirationSeconds())
                    .build();
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return issueTokens(user);
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

    public void changePassword(String email, String currentPassword, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email gerekli");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        // Verify current password
        String sanitizedCurrentPassword = currentPassword != null ? currentPassword.trim() : "";
        if (!passwordEncoder.matches(sanitizedCurrentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mevcut şifre hatalı");
        }

        // Validate new password
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("Yeni şifre en az 8 karakter olmalıdır");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        logger.info("Password changed for user: {}", normalizedEmail);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

    public void setupSmsMfa(String email, String phoneNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
    }

    public boolean verifySmsMfa(String email, String code) {
        throw new UnsupportedOperationException("SMS MFA henüz desteklenmiyor");
    }

    public MfaSetupResponse setupTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate a random secret key (20 bytes = 160 bits, standard for TOTP)
        byte[] secretBytes = new byte[20];
        new java.security.SecureRandom().nextBytes(secretBytes);
        Base32 base32 = new Base32();
        String secret = base32.encodeToString(secretBytes);

        user.setTotpSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        // Generate TOTP URI for QR code
        // Format: otpauth://totp/Issuer:UserEmail?secret=SECRET&issuer=Issuer
        String issuer = "CyberScope";
        String totpUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, email, secret, issuer);

        return new MfaSetupResponse(secret, totpUri);
    }

    public JwtResponse verifyTotpMfa(String email, String totpToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String secret = user.getTotpSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("TOTP MFA not setup for user");
        }

        if (totpToken == null || totpToken.length() != 6 || !totpToken.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid TOTP token");
        }

        int code;
        try {
            code = Integer.parseInt(totpToken);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid TOTP token");
        }

        try {
            if (!totpVerifier.verify(secret, code)) {
                throw new IllegalArgumentException("Invalid TOTP token");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("TOTP verification failed: " + e.getMessage(), e);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    public void enableTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
            throw new IllegalArgumentException("TOTP secret not found. Please setup TOTP first.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public void disableTotpMfa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setTotpSecret(null);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    public JwtResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz yenileme tokenı"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Yenileme tokenının süresi dolmuş");
        }

        User user = refreshToken.getUser();
        return buildJwtResponse(user, generateAccessToken(user), refreshTokenValue);
    }

    private JwtResponse issueTokens(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.deleteAllExpired(Instant.now());

        String accessToken = generateAccessToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();
        Instant refreshExpiry = Instant.now().plusSeconds(refreshTokenValiditySeconds);

        RefreshToken refreshToken = new RefreshToken(user, refreshTokenValue, refreshExpiry);
        refreshTokenRepository.save(refreshToken);

        return buildJwtResponse(user, accessToken, refreshTokenValue);
    }

    private String generateAccessToken(User user) {
        String role = determinePrimaryRole(user);
        return jwtUtil.generateToken(user.getEmail(), Map.of(
                "userId", user.getId(),
                "role", role
        ));
    }

    private JwtResponse buildJwtResponse(User user, String accessToken, String refreshTokenValue) {
        String normalizedRole = determinePrimaryRole(user);
        return JwtResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .mfaRequired(false)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(normalizedRole)
                .verified(Boolean.TRUE.equals(user.getIsVerified()))
                .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
                .expiresIn(jwtUtil.getExpirationSeconds())
                .build();
    }

    private String determinePrimaryRole(User user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            if (user.getRoles().stream().anyMatch(role -> "admin".equalsIgnoreCase(role.getName()))) {
                return "admin";
            }
            if (user.getRoles().stream().anyMatch(role -> "analyst".equalsIgnoreCase(role.getName()))) {
                return "analyst";
            }
            if (user.getRoles().stream().anyMatch(role -> "viewer".equalsIgnoreCase(role.getName()))) {
                return "viewer";
            }

            Optional<String> firstRole = user.getRoles().stream()
                    .map(Role::getName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .findFirst();
            if (firstRole.isPresent()) {
                return normalizeRole(firstRole.get());
            }
        }

        return normalizeRole(user.getRole());
    }

    private Role resolveRoleEntity(String normalizedRole) {
        return roleRepository.findByName(normalizedRole)
                .orElseThrow(() -> new IllegalArgumentException("Rol bulunamadı: " + normalizedRole));
    }

    private void assignRole(User user, String normalizedRole) {
        Role roleEntity = resolveRoleEntity(normalizedRole);
        user.setRole(normalizedRole);
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().clear();
        user.getRoles().add(roleEntity);
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return DEFAULT_ROLE;
        }

        String normalizedRole = role.trim().toLowerCase();
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            logger.warn("Unknown role '{}' provided while building auth payload, defaulting to {}", role, DEFAULT_ROLE);
            return DEFAULT_ROLE;
        }
        return normalizedRole;
    }
}