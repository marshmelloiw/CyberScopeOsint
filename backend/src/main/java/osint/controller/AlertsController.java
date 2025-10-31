package osint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import osint.service.AlertService;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts")
public class AlertsController {
    private final AlertService alertService;

    public AlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/test")
    @Operation(summary = "Send test alert", description = "Triggers email and/or SMS based on configured thresholds")
    public ResponseEntity<?> test(@RequestBody Map<String, Object> body) {
        String subject = (String) body.getOrDefault("subject", "CyberScope Test Alert");
        String message = (String) body.getOrDefault("message", "This is a test alert.");
        int score = ((Number) body.getOrDefault("risk_score", 75)).intValue();
        alertService.handleRiskScore(subject, message, score);
        return ResponseEntity.ok(Map.of("status", "sent_or_below_threshold"));
    }
}
