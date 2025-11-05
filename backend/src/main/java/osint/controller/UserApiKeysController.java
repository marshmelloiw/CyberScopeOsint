package osint.controller;

import osint.model.ApiKey;
import osint.service.UserApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-apikeys")
public class UserApiKeysController {
    
    private final UserApiKeyService userApiKeyService;
    
    @Autowired
    public UserApiKeysController(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }
    
    /**
     * Get all API keys for current user
     */
    @GetMapping
    public ResponseEntity<?> getAllApiKeys(@RequestParam(required = false) Long userId) {
        try {
            List<ApiKey> apiKeys = userApiKeyService.getAllApiKeys(userId);
            List<Map<String, Object>> apiKeysList = apiKeys.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "apiKeys", apiKeysList,
                "total", apiKeysList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key'ler alınamadı: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get API key by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApiKeyById(@PathVariable Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = userApiKeyService.getApiKeyById(id);
            if (!apiKeyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(convertToResponse(apiKeyOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key alınamadı: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Create new API key
     */
    @PostMapping
    public ResponseEntity<?> createApiKey(@RequestBody Map<String, Object> request) {
        try {
            String keyName = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) request.get("permissions");
            String rateLimit = (String) request.get("rateLimit");
            String expiresAtStr = (String) request.get("expiresAt");
            String manualApiKey = (String) request.get("apiKey");
            String manualSecretKey = (String) request.get("secretKey");
            Long userId = request.get("userId") != null ? 
                Long.parseLong(request.get("userId").toString()) : null;
            
            LocalDateTime expiresAt = null;
            if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
                expiresAt = LocalDateTime.parse(expiresAtStr.replace("Z", ""), 
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            ApiKey apiKey = userApiKeyService.createApiKey(
                keyName, description, permissions, rateLimit, expiresAt, userId,
                manualApiKey, manualSecretKey
            );
            
            return ResponseEntity.ok(convertToResponse(apiKey));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key oluşturulamadı: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Update API key
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateApiKey(@PathVariable Long id, 
                                         @RequestBody Map<String, Object> request) {
        try {
            String keyName = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) request.get("permissions");
            String rateLimit = (String) request.get("rateLimit");
            String expiresAtStr = (String) request.get("expiresAt");
            String manualApiKey = (String) request.get("apiKey");
            String manualSecretKey = (String) request.get("secretKey");
            
            LocalDateTime expiresAt = null;
            if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
                expiresAt = LocalDateTime.parse(expiresAtStr.replace("Z", ""), 
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            ApiKey apiKey = userApiKeyService.updateApiKey(
                id, keyName, description, permissions, rateLimit, expiresAt,
                manualApiKey, manualSecretKey
            );
            
            return ResponseEntity.ok(convertToResponse(apiKey));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key güncellenemedi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Delete/Revoke API key
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable Long id) {
        try {
            userApiKeyService.deleteApiKey(id);
            return ResponseEntity.ok(Map.of(
                "message", "API key başarıyla silindi",
                "id", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key silinemedi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Regenerate API key
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<?> regenerateApiKey(@PathVariable Long id) {
        try {
            ApiKey apiKey = userApiKeyService.regenerateApiKey(id);
            return ResponseEntity.ok(convertToResponse(apiKey));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key yenilenemedi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Export API key (returns key and secret for download)
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<?> exportApiKey(@PathVariable Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = userApiKeyService.getApiKeyById(id);
            if (!apiKeyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("name", apiKey.getKeyName());
            exportData.put("apiKey", apiKey.getApiKey());
            exportData.put("secret", apiKey.getSecretKey());
            exportData.put("description", apiKey.getDescription());
            exportData.put("permissions", apiKey.getPermissions());
            exportData.put("rateLimit", apiKey.getRateLimit());
            exportData.put("createdAt", apiKey.getCreatedAt());
            
            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "API key export edilemedi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convert ApiKey entity to response map
     */
    private Map<String, Object> convertToResponse(ApiKey apiKey) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", apiKey.getId());
        response.put("name", apiKey.getKeyName());
        response.put("key", apiKey.getApiKey());
        response.put("secret", apiKey.getSecretKey());
        response.put("status", apiKey.getStatus());
        response.put("description", apiKey.getDescription());
        response.put("permissions", apiKey.getPermissions() != null ? apiKey.getPermissions() : List.of());
        response.put("rateLimit", apiKey.getRateLimit());
        response.put("usageCount", apiKey.getUsageCount() != null ? apiKey.getUsageCount() : 0L);
        response.put("createdAt", apiKey.getCreatedAt() != null ? 
            apiKey.getCreatedAt().toString() : null);
        response.put("lastUsed", apiKey.getLastUsedAt() != null ? 
            apiKey.getLastUsedAt().toString() : null);
        response.put("expiresAt", apiKey.getExpiresAt() != null ? 
            apiKey.getExpiresAt().toString() : null);
        return response;
    }
}

