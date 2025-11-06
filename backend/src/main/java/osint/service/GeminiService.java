package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final WebClient webClient;
    private final String apiKey;

    public GeminiService(@Value("${osint.gemini.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        logger.info("GeminiService - API Key: {}", (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(2));
        
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        this.webClient = builder.build();
    }

    /**
     * Analyzes scan results using Google Gemini AI
     * @param scanResults The combined scan results from multiple providers
     * @param scanType The type of scan (domain, ip, email)
     * @return AI-generated analysis and recommendations
     */
    public Mono<Map<String, Object>> analyzeScanResults(Map<String, Object> scanResults, String scanType) {
        if (apiKey.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Gemini API key not configured");
            return Mono.just(error);
        }

        try {
            String prompt = buildAnalysisPrompt(scanResults, scanType);
            
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            contents.put("parts", java.util.List.of(part));
            requestBody.put("contents", java.util.List.of(contents));

            logger.info("Sending analysis request to Gemini for scan type: {}", scanType);
            logger.debug("Request body: {}", requestBody);

            return webClient.post()
                    .uri("/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        logger.error("Gemini API returned error status: {}", response.statusCode());
                        return Mono.error(new RuntimeException("Gemini API error: " + response.statusCode()));
                    })
                    .bodyToMono(Map.class)
                    .doOnNext(response -> logger.info("Received Gemini response: {}", response))
                    .map(response -> parseGeminiResponse((Map<String, Object>) response))
                    .doOnNext(result -> logger.info("Parsed Gemini result: {}", result))
                    .doOnError(error -> logger.error("Gemini API error", error))
                    .onErrorResume(error -> {
                        logger.error("Gemini API request failed: {}", error.getMessage(), error);
                        return Mono.just(createErrorMap("Failed to analyze with Gemini: " + error.getMessage()));
                    });
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            return Mono.just(createErrorMap("Error calling Gemini: " + e.getMessage()));
        }
    }

    private String buildAnalysisPrompt(Map<String, Object> scanResults, String scanType) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a senior cybersecurity expert analyzing OSINT scan results. ");
        prompt.append("Provide a comprehensive, detailed security analysis report in text format.\n\n");
        prompt.append("Your report should include:\n");
        prompt.append("1. Executive Summary: A 2-3 sentence overview of the overall security posture\n");
        prompt.append("2. Risk Assessment: Risk level (LOW, MEDIUM, HIGH, CRITICAL) and score (0-10)\n");
        prompt.append("3. Key Findings: Important security discoveries from the scan\n");
        prompt.append("4. Identified Vulnerabilities: All security vulnerabilities found\n");
        prompt.append("5. Security Recommendations: Actionable steps to improve security\n");
        prompt.append("6. Threat Indicators: Any suspicious or concerning indicators\n\n");
        
        prompt.append("Scan Type: ").append(scanType.toUpperCase()).append("\n\n");
        prompt.append("Scan Results Data:\n");
        prompt.append(formatResultsForPrompt(scanResults));
        prompt.append("\n\nProvide a detailed, professional security analysis based on the above data. ");
        prompt.append("Be thorough and focus on actionable insights.");

        return prompt.toString();
    }

    private String formatResultsForPrompt(Map<String, Object> results) {
        StringBuilder formatted = new StringBuilder();
        try {
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                formatted.append("\n[").append(entry.getKey()).append("]\n");
                formatted.append(entry.getValue().toString()).append("\n");
            }
        } catch (Exception e) {
            formatted.append(results.toString());
        }
        return formatted.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGeminiResponse(Map<String, Object> response) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!response.containsKey("candidates")) {
                logger.error("Gemini response missing 'candidates' field");
                return createErrorMap("Invalid response from Gemini API");
            }

            Object candidates = response.get("candidates");
            if (candidates instanceof java.util.List && !((java.util.List<?>) candidates).isEmpty()) {
                Object firstCandidate = ((java.util.List<?>) candidates).get(0);
                if (firstCandidate instanceof Map) {
                    Map<String, Object> candidate = (Map<String, Object>) firstCandidate;
                    if (candidate.containsKey("content")) {
                        Object content = candidate.get("content");
                        if (content instanceof Map) {
                            Map<String, Object> contentMap = (Map<String, Object>) content;
                            if (contentMap.containsKey("parts")) {
                                Object parts = contentMap.get("parts");
                                if (parts instanceof java.util.List && !((java.util.List<?>) parts).isEmpty()) {
                                    Object firstPart = ((java.util.List<?>) parts).get(0);
                                    if (firstPart instanceof Map) {
                                        Map<String, Object> partMap = (Map<String, Object>) firstPart;
                                        if (partMap.containsKey("text")) {
                                            String text = String.valueOf(partMap.get("text"));
                                            
                                            // Try to parse JSON from the response
                                            try {
                                                ObjectMapper mapper = new ObjectMapper();
                                                Map<String, Object> parsed = mapper.readValue(text, Map.class);
                                                result.put("analysis", parsed);
                                                result.put("raw_text", text);
                                            } catch (Exception e) {
                                                // If JSON parsing fails, return raw text as analysis
                                                logger.warn("Failed to parse Gemini JSON response, returning raw text as analysis");
                                                // Always provide analysis field - use raw_text if JSON parsing fails
                                                result.put("analysis", text);
                                                result.put("raw_text", text);
                                                result.put("note", "Response format is plain text, not JSON");
                                            }
                                            return result;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            logger.error("Could not extract text from Gemini response");
            return createErrorMap("Could not extract analysis from response");
        } catch (Exception e) {
            logger.error("Error parsing Gemini response", e);
            return createErrorMap("Error parsing response: " + e.getMessage());
        }
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}

