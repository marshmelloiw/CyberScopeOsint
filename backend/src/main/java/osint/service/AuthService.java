package osint.service;

import osint.dto.JwtResponse;
import osint.repository.PasswordResetTokenRepository;
import osint.repository.RoleRepository;
import osint.repository.UserRepository;
import osint.model.PasswordResetToken;
import osint.model.Role;
import osint.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public void register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        String hash = passwordEncoder.encode(rawPassword);
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        User user = User.builder()
                .email(email)
                .passwordHash(hash)
                .roles(Set.of(userRole))
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
        user.setPhoneNumber(phoneNumber);
        user.setSmsMfaEnabled(true);
        userRepository.save(user);
    }

    public boolean verifySmsMfa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getSmsMfaEnabled())) {
            throw new IllegalArgumentException("SMS MFA not enabled for user");
        }

        // Mock SMS verification - in production, verify against SMS service
        return "123456".equals(code);
    }
}