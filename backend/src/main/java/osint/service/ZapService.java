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
public class ZapService {

    private static final Logger logger = LoggerFactory.getLogger(ZapService.class);
    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;

    public ZapService(@Value("${osint.zap.api-key:}") String apiKey,
                     @Value("${osint.zap.base-url:http://localhost:8080}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        logger.info("ZapService - API Key: {}", (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));
        logger.info("ZapService - Base URL: {}", baseUrl);

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl);

        // Sadece API key varsa header ekle
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("X-ZAP-API-Key", apiKey);
        }

        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> scanUrl(String url) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        return webClient.get()
                .uri("/JSON/spider/action/scan/?url={url}", url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to scan URL with ZAP"));
    }

    public Mono<Map<String, Object>> getSpiderStatus(String scanId) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        return webClient.get()
                .uri("/JSON/spider/view/status/?scanId={scanId}", scanId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to get spider status from ZAP"));
    }

    public Mono<Map<String, Object>> getActiveScanResults(String scanId) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        return webClient.get()
                .uri("/JSON/ascan/view/status/?scanId={scanId}", scanId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to get active scan results from ZAP"));
    }

    public Mono<Map<String, Object>> getAlerts(String baseUrl) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        return webClient.get()
                .uri("/JSON/core/view/alerts/?baseurl={baseUrl}", baseUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to get alerts from ZAP"));
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
