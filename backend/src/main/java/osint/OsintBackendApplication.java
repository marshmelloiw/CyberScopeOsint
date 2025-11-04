package osint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import osint.service.AuthService;
import osint.repository.UserRepository;
import osint.model.User;

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
                    authService.register(adminEmail, adminPassword);
                    User newAdmin = userRepository.findByEmail(adminEmail).orElse(null);
                    if (newAdmin != null) {
                        // Set admin role
                        newAdmin.setRole("ADMIN");
                        newAdmin.setIsVerified(true);
                        return userRepository.save(newAdmin);
                    }
                    return null;
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            });

            // Update existing admin user role if needed
            if (admin != null && !"ADMIN".equals(admin.getRole())) {
                admin.setRole("ADMIN");
                admin.setIsVerified(true);
                userRepository.save(admin);
            }
        };
    }
}

