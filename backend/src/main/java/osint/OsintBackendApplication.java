package osint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import osint.service.AuthService;
import osint.repository.UserRepository;
import osint.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class OsintBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsintBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDefaults(
            UserRepository userRepository,
            AuthService authService,
            @Value("${ADMIN_EMAIL:admin@example.com}") String adminEmail,
            @Value("${ADMIN_PASSWORD:Admin123!}") String adminPassword) {
        return args -> {
            // Create admin user if it doesn't exist
            User admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
                try {
                    // Create admin user directly (no file required for default admin)
                    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                    String hash = passwordEncoder.encode(adminPassword);
                    User newAdmin = User.builder()
                            .email(adminEmail)
                            .fullName("Admin User")
                            .passwordHash(hash)
                            .role("ADMIN")
                            .isVerified(true)
                            .mfaEnabled(false)
                            .build();
                    return userRepository.save(newAdmin);
                } catch (Exception ignored) {
                    return null;
                }
            });

            // Update existing admin user role and verification status if needed
            if (admin != null) {
                boolean needsUpdate = false;
                if (!"ADMIN".equals(admin.getRole())) {
                    admin.setRole("ADMIN");
                    needsUpdate = true;
                }
                if (admin.getIsVerified() == null || !admin.getIsVerified()) {
                    admin.setIsVerified(true);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    userRepository.save(admin);
                }
            }
        };
    }
}