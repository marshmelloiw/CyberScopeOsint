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
public class VirusTotalService {

    private static final Logger logger = LoggerFactory.getLogger(VirusTotalService.class);
    private final WebClient webClient;
    private final String apiKey;

    public VirusTotalService(@Value("${osint.virustotal.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        logger.info("VirusTotalService - API Key: {}", (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://www.virustotal.com/api/v3");

        // Sadece API key varsa header ekle
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("x-apikey", apiKey);
        }

        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> getIpReport(String ip) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("VirusTotal API key not configured"));
        }

        return webClient.get()
                .uri("/ip_addresses/{ip}", ip)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to fetch IP report from VirusTotal"));
    }

    public Mono<Map<String, Object>> getDomainReport(String domain) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("VirusTotal API key not configured"));
        }

        return webClient.get()
                .uri("/domains/{domain}", domain)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to fetch domain report from VirusTotal"));
    }

    public Mono<Map<String, Object>> getUrlReport(String url) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("VirusTotal API key not configured"));
        }

        return webClient.get()
                .uri("/urls/{url}", url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to fetch URL report from VirusTotal"));
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}