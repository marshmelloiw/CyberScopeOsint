package osint.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import osint.service.ThreatScoringClient;

@RestController
@RequestMapping("/api/threat")
@Tag(name = "Threat Intelligence")
public class ThreatController {
    private final ThreatScoringClient client;

    public ThreatController(ThreatScoringClient client) {
        this.client = client;
    }

    @PostMapping("/score")
    @Operation(summary = "Compute AI-driven threat score", description = "Sends provider outcomes to AI service and returns risk score with recommendations.")
    public ResponseEntity<?> score(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = client.score(payload);
        return ResponseEntity.ok(result);
    }
}
