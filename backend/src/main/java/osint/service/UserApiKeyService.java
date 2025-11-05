package osint.service;

import osint.model.ApiKey;
import osint.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class UserApiKeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserApiKeyService.class);
    private static final String API_KEY_PREFIX = "cy_sk_";
    private static final SecureRandom secureRandom = new SecureRandom();
    
    private final ApiKeyRepository apiKeyRepository;
    
    @Autowired
    public UserApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    /**
     * Generate a secure random API key
     */
    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return API_KEY_PREFIX + base64.substring(0, 24);
    }
    
    /**
     * Generate a secure random secret key
     */
    private String generateSecretKey() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Create a new API key
     */
    @Transactional
    public ApiKey createApiKey(String keyName, String description, List<String> permissions, 
                              String rateLimit, LocalDateTime expiresAt, Long userId,
                              String manualApiKey, String manualSecretKey) {
        try {
            ApiKey apiKey = new ApiKey();
            apiKey.setKeyName(keyName);
            // Use manual key if provided, otherwise generate
            apiKey.setApiKey((manualApiKey != null && !manualApiKey.trim().isEmpty()) 
                ? manualApiKey.trim() : generateApiKey());
            apiKey.setSecretKey((manualSecretKey != null && !manualSecretKey.trim().isEmpty()) 
                ? manualSecretKey.trim() : generateSecretKey());
            apiKey.setDescription(description);
            apiKey.setPermissions(permissions);
            apiKey.setRateLimit(rateLimit != null ? rateLimit : "1000/hour");
            apiKey.setExpiresAt(expiresAt);
            apiKey.setUserId(userId);
            apiKey.setStatus("active");
            apiKey.setUsageCount(0L);
            
            apiKey = apiKeyRepository.save(apiKey);
            logger.info("Created API key: {}", apiKey.getId());
            
            return apiKey;
        } catch (Exception e) {
            logger.error("Error creating API key: {}", e.getMessage(), e);
            throw new RuntimeException("API key oluşturulamadı: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all API keys (for current user or all if admin)
     */
    public List<ApiKey> getAllApiKeys(Long userId) {
        try {
            // For now, return all keys. In production, filter by userId
            // if (userId != null) {
            //     return apiKeyRepository.findByUserId(userId);
            // }
            return apiKeyRepository.findAllOrderByCreatedAtDesc();
        } catch (Exception e) {
            logger.error("Error getting API keys: {}", e.getMessage(), e);
            throw new RuntimeException("API key'ler alınamadı: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get API key by ID
     */
    public Optional<ApiKey> getApiKeyById(Long id) {
        return apiKeyRepository.findById(id);
    }
    
    /**
     * Update API key
     */
    @Transactional
    public ApiKey updateApiKey(Long id, String keyName, String description, 
                              List<String> permissions, String rateLimit, 
                              LocalDateTime expiresAt, String manualApiKey, 
                              String manualSecretKey) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            if (!apiKeyOpt.isPresent()) {
                throw new RuntimeException("API key bulunamadı");
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            
            // Update all fields if provided (null check ensures only provided fields are updated)
            if (keyName != null) {
                apiKey.setKeyName(keyName);
            }
            if (description != null) {
                apiKey.setDescription(description);
            }
            if (permissions != null) {
                apiKey.setPermissions(permissions);
            }
            if (rateLimit != null) {
                apiKey.setRateLimit(rateLimit);
            }
            if (expiresAt != null) {
                apiKey.setExpiresAt(expiresAt);
            }
            
            // Update API key and secret if provided (not null and not empty)
            if (manualApiKey != null && !manualApiKey.trim().isEmpty()) {
                apiKey.setApiKey(manualApiKey.trim());
                logger.debug("Updated API key value for ID: {}", id);
            }
            if (manualSecretKey != null && !manualSecretKey.trim().isEmpty()) {
                apiKey.setSecretKey(manualSecretKey.trim());
                logger.debug("Updated secret key value for ID: {}", id);
            }
            
            // Save to database - JPA will persist all changes
            apiKey = apiKeyRepository.save(apiKey);
            logger.info("Updated API key: {} - All changes saved to database", id);
            
            return apiKey;
        } catch (Exception e) {
            logger.error("Error updating API key {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("API key güncellenemedi: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete/Revoke API key
     */
    @Transactional
    public void deleteApiKey(Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            if (!apiKeyOpt.isPresent()) {
                throw new RuntimeException("API key bulunamadı");
            }
            
            apiKeyRepository.delete(apiKeyOpt.get());
            logger.info("Deleted API key: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting API key {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("API key silinemedi: " + e.getMessage(), e);
        }
    }
    
    /**
     * Regenerate API key (generate new key and secret, keep same record)
     */
    @Transactional
    public ApiKey regenerateApiKey(Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            if (!apiKeyOpt.isPresent()) {
                throw new RuntimeException("API key bulunamadı");
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setApiKey(generateApiKey());
            apiKey.setSecretKey(generateSecretKey());
            apiKey.setUsageCount(0L);
            
            apiKey = apiKeyRepository.save(apiKey);
            logger.info("Regenerated API key: {}", id);
            
            return apiKey;
        } catch (Exception e) {
            logger.error("Error regenerating API key {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("API key yenilenemedi: " + e.getMessage(), e);
        }
    }
    
    /**
     * Increment usage count and update last used timestamp
     */
    @Transactional
    public void incrementUsageCount(String apiKeyValue) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue);
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setUsageCount(apiKey.getUsageCount() + 1);
                apiKey.setLastUsedAt(LocalDateTime.now());
                apiKeyRepository.save(apiKey);
            }
        } catch (Exception e) {
            logger.warn("Error incrementing usage count for API key: {}", e.getMessage());
        }
    }
    
    /**
     * Check if API key is valid (exists, active, not expired)
     */
    public boolean isValidApiKey(String apiKeyValue) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue);
            if (!apiKeyOpt.isPresent()) {
                return false;
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            
            // Check status
            if (!"active".equalsIgnoreCase(apiKey.getStatus())) {
                return false;
            }
            
            // Check expiration
            if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating API key: {}", e.getMessage(), e);
            return false;
        }
    }
}

