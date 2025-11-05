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

    /**
     * Comprehensive ZAP scan that retrieves alerts
     * Returns full scan results with all ZAP data
     */
    public Mono<Map<String, Object>> performComprehensiveScan(String url) {
        if (apiKey == null || apiKey.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "ZAP API key not configured");
            errorResult.put("url", url);
            return Mono.just(errorResult);
        }

        logger.info("Starting ZAP scan for URL: {}", url);

        // Get alerts - return everything ZAP gives us
        return getAlerts(url)
                .map(alertsResponse -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    // Add URL and timestamp
                    result.put("url", url);
                    result.put("scanned_at", System.currentTimeMillis());
                    
                    // Add the entire alerts response - preserve all fields
                    if (alertsResponse != null) {
                        if (alertsResponse instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> alertsMap = (Map<String, Object>) alertsResponse;
                            // Add every field from alerts response
                            for (Map.Entry<String, Object> entry : alertsMap.entrySet()) {
                                result.put(entry.getKey(), entry.getValue());
                            }
                        }
                        // Always keep the full response as well
                        result.put("zap_response", alertsResponse);
                    }
                    
                    // Also try to start a spider scan
                    scanUrl(url).subscribe(
                        scanResponse -> logger.debug("ZAP spider scan started: {}", scanResponse),
                        error -> logger.debug("ZAP spider scan failed: {}", error.getMessage())
                    );
                    
                    result.put("status", "completed");
                    logger.info("ZAP scan result for {} contains {} fields", url, result.size());
                    return result;
                })
                .onErrorResume(error -> {
                    logger.error("ZAP scan error for {}: {}", url, error.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "ZAP scan failed: " + error.getMessage());
                    errorResult.put("url", url);
                    errorResult.put("timestamp", System.currentTimeMillis());
                    return Mono.just(errorResult);
                });
    }


    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
