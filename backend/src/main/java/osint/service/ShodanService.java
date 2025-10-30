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
public class ShodanService {

    private static final Logger logger = LoggerFactory.getLogger(ShodanService.class);
    private final WebClient webClient;
    private final String apiKey;

    public ShodanService(@Value("${osint.shodan.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        logger.info("ShodanService - API Key: {}", (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.shodan.io");

        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> getHostInfo(String ip) {
        if (apiKey.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Shodan API key not configured");
            return Mono.just(error);
        }

        return webClient.get()
                .uri("/shodan/host/{ip}?key={key}", ip, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to fetch data from Shodan"));
    }

    public Mono<Map<String, Object>> searchHosts(String query) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("Shodan API key not configured"));
        }

        return webClient.get()
                .uri("/shodan/host/search?key={key}&query={query}", apiKey, query)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to search Shodan"));
    }

    public Mono<Map<String, Object>> getDomainInfo(String domain) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("Shodan API key not configured"));
        }

        return webClient.get()
                .uri("/dns/domain/{domain}?key={key}", domain, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to fetch domain info from Shodan"));
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}