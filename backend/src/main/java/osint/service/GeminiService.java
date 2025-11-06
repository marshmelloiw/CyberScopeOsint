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
        prompt.append("Provide a comprehensive, detailed security analysis report in GitHub-flavored Markdown format.\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use Markdown headings (##, ###) for each section.\n");
        prompt.append("- Use bullet lists or numbered lists where appropriate.\n");
        prompt.append("- Do NOT return HTML or JSON.\n");
        prompt.append("- Focus on actionable, professional insights.\n\n");
        prompt.append("Your report must include the following sections:\n");
        prompt.append("1. ## Executive Summary\n");
        prompt.append("2. ## Risk Assessment (include level LOW/MEDIUM/HIGH/CRITICAL and score 0-10)\n");
        prompt.append("3. ## Key Findings\n");
        prompt.append("4. ## Identified Vulnerabilities\n");
        prompt.append("5. ## Security Recommendations\n");
        prompt.append("6. ## Threat Indicators\n\n");
        
        prompt.append("Scan Type: ").append(scanType.toUpperCase()).append("\n\n");
        prompt.append("Scan Results Data:\n");
        prompt.append(formatResultsForPrompt(scanResults));
        prompt.append("\n\nProvide the Markdown report now.");

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
                                                result.put("markdown", convertStructuredReportToMarkdown(parsed));
                                            } catch (Exception e) {
                                                logger.warn("Failed to parse Gemini JSON response, returning raw text as markdown");
                                                result.put("analysis", text);
                                                result.put("raw_text", text);
                                                result.put("markdown", text);
                                                result.put("note", "Response format treated as Markdown text");
                                            }
                                            result.put("format", "markdown");
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

    private String convertStructuredReportToMarkdown(Map<String, Object> data) {
        StringBuilder markdown = new StringBuilder();
        data.forEach((key, value) -> {
            String heading = formatTitle(key);
            if (!heading.isBlank()) {
                markdown.append("## ").append(heading).append("\n\n");
            }
            appendValueMarkdown(markdown, value, 0);
            markdown.append("\n\n");
        });
        String output = markdown.toString().replaceAll("\n{3,}", "\n\n").trim();
        return output.isEmpty() ? "*(no data)*" : output;
    }

    @SuppressWarnings("unchecked")
    private void appendValueMarkdown(StringBuilder sb, Object value, int indentLevel) {
        if (value == null) {
            sb.append(indent(indentLevel)).append("- *(no data)*\n");
            return;
        }

        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append(indent(indentLevel)).append("- *(no data)*\n");
                return;
            }
            map.forEach((k, v) -> {
                String title = formatTitle(String.valueOf(k));
                if (isSimpleValue(v)) {
                    sb.append(indent(indentLevel)).append("- **").append(title).append("**: ")
                            .append(formatSimple(v)).append("\n");
                } else {
                    sb.append(indent(indentLevel)).append("- **").append(title).append("**\n");
                    appendValueMarkdown(sb, v, indentLevel + 1);
                }
            });
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            boolean empty = true;
            for (Object item : iterable) {
                empty = false;
                if (isSimpleValue(item)) {
                    sb.append(indent(indentLevel)).append("- ").append(formatSimple(item)).append("\n");
                } else {
                    sb.append(indent(indentLevel)).append("-\n");
                    appendValueMarkdown(sb, item, indentLevel + 1);
                }
            }
            if (empty) {
                sb.append(indent(indentLevel)).append("- *(no data)*\n");
            }
            return;
        }

        sb.append(indent(indentLevel)).append(formatSimple(value)).append("\n");
    }

    private String indent(int level) {
        return "  ".repeat(Math.max(0, level));
    }

    private boolean isSimpleValue(Object value) {
        return value == null || value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private String formatSimple(Object value) {
        if (value == null) {
            return "*(no data)*";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "Yes" : "No";
        }
        return String.valueOf(value).trim();
    }

    private String formatTitle(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String withSpaces = key.replaceAll("[_-]", " ")
                .replaceAll("(?<!^)([A-Z])", " $1");
        String cleaned = withSpaces.trim();
        return cleaned.isEmpty() ? key : cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
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

