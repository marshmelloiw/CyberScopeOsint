package osint.controller;

import osint.dto.*;
import osint.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            JwtResponse jwt = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(jwt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage())); // e.getMessage() → “Hesabınız aktif değil.”
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
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

    @PostMapping("/mfa/setup")
    public ResponseEntity<osint.dto.MfaSetupResponse> setupTotpMfa(
            @Valid @RequestBody osint.dto.MfaSetupRequest request) {
        try {
            osint.dto.MfaSetupResponse response = authService.setupTotpMfa(request.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyTotpMfa(@Valid @RequestBody osint.dto.MfaVerifyRequest request) {
        try {
            boolean isValid = authService.verifyTotpMfa(request.getUsername(), request.getTotpToken());
            if (isValid) {
                authService.enableTotpMfa(request.getUsername());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableTotpMfa(@Valid @RequestBody osint.dto.MfaSetupRequest request) {
        try {
            authService.disableTotpMfa(request.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}