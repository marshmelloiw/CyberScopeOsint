package osint.controller;

import osint.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/apikeys")
public class ApiKeysController {
    private final ApiKeyService apiKeyService;

    public ApiKeysController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public Map<String, Object> getKeys() {
        return Map.of(
                "shodan", !apiKeyService.getShodanKey().isEmpty(),
                "virustotal", !apiKeyService.getVirusTotalKey().isEmpty(),
                "hibp", !apiKeyService.getHibpKey().isEmpty()
        );
    }

    @PostMapping
    public ResponseEntity<?> setKeys(@RequestBody Map<String, String> body) {
        if (body.containsKey("shodan")) apiKeyService.setShodanKey(body.get("shodan"));
        if (body.containsKey("virustotal")) apiKeyService.setVirusTotalKey(body.get("virustotal"));
        if (body.containsKey("hibp")) apiKeyService.setHibpKey(body.get("hibp"));
        return ResponseEntity.ok().build();
    }
}


