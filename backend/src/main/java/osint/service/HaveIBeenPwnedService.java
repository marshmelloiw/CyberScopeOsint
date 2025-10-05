package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class HaveIBeenPwnedService {

    private static final Logger logger = LoggerFactory.getLogger(HaveIBeenPwnedService.class);
    private final WebClient webClient;
    private final String apiKey;

    public HaveIBeenPwnedService(@Value("${osint.hibp.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        logger.info("HaveIBeenPwnedService - API Key: {}",
                (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_actual_hibp_api_key_here") ? "SET"
                        : "NOT SET"));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://haveibeenpwned.com/api/v3");

        // Sadece API key varsa header ekle
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_actual_hibp_api_key_here")) {
            builder.defaultHeader("hibp-api-key", apiKey);
        }

        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> checkEmailBreach(String email) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your_actual_hibp_api_key_here")) {
            return Mono.just(createErrorMap("HaveIBeenPwned API key not configured"));
        }

        return webClient.get()
                .uri("/breachedaccount/{email}", email)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .onErrorReturn(createErrorMap("Failed to check email breach from HaveIBeenPwned"));
    }

    public Mono<Map<String, Object>> getBreachDetails(String breachName) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your_actual_hibp_api_key_here")) {
            return Mono.just(createErrorMap("HaveIBeenPwned API key not configured"));
        }

        return webClient.get()
                .uri("/breach/{breachName}", breachName)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    return typedResponse;
                })
                .onErrorReturn(createErrorMap("Failed to fetch breach details from HaveIBeenPwned"));
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}