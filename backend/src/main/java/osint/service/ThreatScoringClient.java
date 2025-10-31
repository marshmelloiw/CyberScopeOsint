package osint.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ThreatScoringClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    private final WebClient webClient;

    public ThreatScoringClient(@Value("${AI_SERVICE_URL:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Map<String, Object> score(Map<String, Object> payload) {
        return this.webClient.post()
                .uri("/score")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MAP_REF)
                .timeout(Duration.ofSeconds(5))
                .block();
    }
}
