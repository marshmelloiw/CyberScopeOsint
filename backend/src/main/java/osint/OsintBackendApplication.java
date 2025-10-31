package osint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;

import osint.model.Role;
import osint.service.AuthService;
import osint.repository.RoleRepository;
import osint.repository.UserRepository;
import osint.model.User;

@SpringBootApplication
@EnableScheduling
public class OsintBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsintBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDefaults(
            RoleRepository roleRepository,
            UserRepository userRepository,
            AuthService authService,
            @Value("${ADMIN_EMAIL:admin@example.com}") String adminEmail,
            @Value("${ADMIN_PASSWORD:Admin123!}") String adminPassword) {
        return args -> {
            roleRepository.findByName("BIREYSEL").orElseGet(() -> roleRepository.save(Role.builder().name("BIREYSEL").build()));
            roleRepository.findByName("KURUMSAL").orElseGet(() -> roleRepository.save(Role.builder().name("KURUMSAL").build()));
            roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

            User admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
                try {
                    authService.register(adminEmail, adminPassword);
                } catch (IllegalArgumentException ignored) {}
                return userRepository.findByEmail(adminEmail).orElse(null);
            });

            if (admin != null) {
                roleRepository.findByName("ADMIN").ifPresent(adminRole -> {
                    if (!admin.getRoles().contains(adminRole)) {
                        admin.getRoles().add(adminRole);
                        userRepository.save(admin);
                    }
                });
            }
        };
    }
}

