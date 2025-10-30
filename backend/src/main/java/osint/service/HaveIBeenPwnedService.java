package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

@Service
public class HaveIBeenPwnedService {

    private static final Logger logger = LoggerFactory.getLogger(HaveIBeenPwnedService.class);
    private final WebClient webClient;
    private final String apiKey;
    private final ApiKeyService apiKeyService;

    public HaveIBeenPwnedService(@Value("${osint.hibp.api-key:}") String apiKey, ApiKeyService apiKeyService) {
        this.apiKey = apiKey;
        this.apiKeyService = apiKeyService;
        logger.info("HaveIBeenPwnedService - API Key: {}",
                (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_actual_hibp_api_key_here") ? "SET"
                        : "NOT SET"));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://haveibeenpwned.com/api/v3");

        // Header request bazında eklenecek

        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> checkEmailBreach(String email) {
        String key = (apiKeyService != null && !apiKeyService.getHibpKey().isEmpty()) ? apiKeyService.getHibpKey()
                : apiKey;
        if (key == null || key.isEmpty()) {
            return Mono.just(createErrorMap("HaveIBeenPwned API key not configured"));
        }

        return webClient.get()
                .uri("/breachedaccount/{email}", email)
                .header("hibp-api-key", key)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorMap("Failed to check email breach from HaveIBeenPwned"));
    }

    public Mono<Map<String, Object>> getBreachDetails(String breachName) {
        String key = (apiKeyService != null && !apiKeyService.getHibpKey().isEmpty()) ? apiKeyService.getHibpKey()
                : apiKey;
        if (key == null || key.isEmpty()) {
            return Mono.just(createErrorMap("HaveIBeenPwned API key not configured"));
        }

        return webClient.get()
                .uri("/breach/{breachName}", breachName)
                .header("hibp-api-key", key)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(createErrorMap("Failed to fetch breach details from HaveIBeenPwned"));
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}