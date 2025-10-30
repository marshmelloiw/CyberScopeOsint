package osint.controller;

import osint.dto.*;
import osint.service.AuthService;
import osint.repository.UserRepository;
import osint.repository.RoleRepository;
import osint.repository.PasswordResetTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController() {
        // manual wiring for in-memory setup
        this.authService = new AuthService(new UserRepository(), new RoleRepository(),
                new PasswordResetTokenRepository());

        // Seed default admin user on startup (idempotent)
        try {
            authService.register("admin@example.com", "Admin123!");
        } catch (IllegalArgumentException ignored) {
            // email already exists -> ignore
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwt = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        authService.deleteUser(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/sms/setup")
    public ResponseEntity<?> setupSmsMfa(@Valid @RequestBody SmsMfaSetupRequest request) {
        // For demo purposes, we'll use a hardcoded email
        // In production, this would come from the authenticated user's context
        authService.setupSmsMfa("admin@example.com", request.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/sms/verify")
    public ResponseEntity<?> verifySmsMfa(@Valid @RequestBody SmsMfaVerifyRequest request) {
        // For demo purposes, we'll use a hardcoded email
        // In production, this would come from the authenticated user's context
        boolean isValid = authService.verifySmsMfa("admin@example.com", request.getCode());
        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}