package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ShodanService {

    private static final Logger logger = LoggerFactory.getLogger(ShodanService.class);
    private final WebClient webClient;
    private final String apiKey;
    
    // IP address validation pattern
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

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

        // Validate IP address format
        if (ip == null || ip.trim().isEmpty()) {
            return Mono.just(createErrorMap("IP address cannot be empty"));
        }
        
        String cleanIp = ip.trim();
        if (!IP_PATTERN.matcher(cleanIp).matches()) {
            return Mono.just(createErrorMap("Invalid IP address format: " + cleanIp));
        }

        logger.info("Fetching Shodan host info for IP: {}", cleanIp);
        
        return webClient.get()
                .uri("/shodan/host/{ip}?key={key}", cleanIp, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    logger.debug("Shodan API response received for IP: {}", cleanIp);
                    return (Map<String, Object>) response;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("Shodan API error for IP {}: Status={}, Body={}", 
                        cleanIp, ex.getStatusCode(), ex.getResponseBodyAsString());
                    
                    String errorMessage = parseShodanError(ex, cleanIp);
                    
                    return Mono.just(createErrorMap(errorMessage));
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error fetching Shodan data for IP {}: {}", cleanIp, ex.getMessage(), ex);
                    return Mono.just(createErrorMap("Failed to fetch data from Shodan: " + ex.getMessage()));
                });
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public Mono<Map<String, Object>> searchHosts(String query) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("Shodan API key not configured"));
        }

        logger.info("Searching Shodan with query: {}", query);
        
        return webClient.get()
                .uri("/shodan/host/search?key={key}&query={query}", apiKey, query)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    logger.debug("Shodan search response received");
                    return (Map<String, Object>) response;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("Shodan search error: Status={}, Body={}", 
                        ex.getStatusCode(), ex.getResponseBodyAsString());
                    String errorMessage = parseShodanError(ex, null);
                    return Mono.just(createErrorMap(errorMessage));
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error searching Shodan: {}", ex.getMessage(), ex);
                    return Mono.just(createErrorMap("Failed to search Shodan: " + ex.getMessage()));
                });
    }

    public Mono<Map<String, Object>> getDomainInfo(String domain) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("Shodan API key not configured"));
        }

        if (domain == null || domain.trim().isEmpty()) {
            return Mono.just(createErrorMap("Domain cannot be empty"));
        }
        
        String cleanDomain = domain.trim();
        logger.info("Fetching Shodan domain info for: {}", cleanDomain);

        // First, try the DNS domain endpoint
        return webClient.get()
                .uri("/dns/domain/{domain}?key={key}", cleanDomain, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    logger.debug("Shodan domain response received for: {}", cleanDomain);
                    Map<String, Object> result = new HashMap<>((Map<String, Object>) response);
                    result.put("domain", cleanDomain);
                    result.put("source", "dns_domain_endpoint");
                    return result;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    int statusCode = ex.getStatusCode().value();
                    
                    // If 403 (forbidden), the DNS domain endpoint is not available in this API plan
                    // Try alternative: resolve domain to IP and get host info
                    if (statusCode == 403) {
                        logger.warn("Shodan DNS domain endpoint not available (403). Trying alternative: resolve domain to IP for: {}", cleanDomain);
                        return resolveDomainToIpAndGetHostInfo(cleanDomain);
                    }
                    
                    logger.error("Shodan domain error for {}: Status={}, Body={}", 
                        cleanDomain, ex.getStatusCode(), ex.getResponseBodyAsString());
                    String errorMessage = parseShodanError(ex, cleanDomain);
                    
                    // Add helpful message for 403 errors
                    if (statusCode == 403) {
                        errorMessage = "Shodan DNS domain endpoint is not available with your API plan. " +
                                      "This endpoint requires a paid Shodan API subscription. " +
                                      "Alternative: Domain was resolved to IP and host info was attempted.";
                    }
                    
                    return Mono.just(createErrorMap(errorMessage));
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error fetching Shodan domain info for {}: {}", 
                        cleanDomain, ex.getMessage(), ex);
                    return Mono.just(createErrorMap("Failed to fetch domain info from Shodan: " + ex.getMessage()));
                });
    }
    
    /**
     * Alternative method: Resolve domain to IP address and get host info
     * This works with free/basic Shodan API plans
     */
    private Mono<Map<String, Object>> resolveDomainToIpAndGetHostInfo(String domain) {
        try {
            logger.info("Resolving domain {} to IP address", domain);
            InetAddress address = InetAddress.getByName(domain);
            String ip = address.getHostAddress();
            
            logger.info("Domain {} resolved to IP: {}", domain, ip);
            
            // Get host info for the resolved IP
            return getHostInfo(ip)
                .map(hostInfo -> {
                    Map<String, Object> result = new HashMap<>(hostInfo);
                    result.put("domain", domain);
                    result.put("resolved_ip", ip);
                    result.put("source", "domain_resolved_to_ip");
                    result.put("note", "Domain endpoint not available. Used IP resolution as alternative.");
                    return result;
                })
                .onErrorResume(ex -> {
                    logger.error("Failed to get host info for resolved IP {}: {}", ip, ex.getMessage());
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Shodan DNS domain endpoint requires paid API plan. " +
                                      "Attempted to resolve domain to IP (" + ip + ") but failed: " + ex.getMessage());
                    error.put("domain", domain);
                    error.put("resolved_ip", ip);
                    return Mono.just(error);
                });
        } catch (Exception e) {
            logger.error("Failed to resolve domain {} to IP: {}", domain, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Shodan DNS domain endpoint requires paid API plan. " +
                              "Failed to resolve domain to IP as alternative: " + e.getMessage());
            error.put("domain", domain);
            return Mono.just(error);
        }
    }

    private String parseShodanError(WebClientResponseException ex, String resource) {
        String errorMessage = "Failed to fetch data from Shodan";
        
        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                // Try to parse JSON error response
                if (responseBody.contains("\"error\"")) {
                    // Extract error message from JSON
                    int errorIndex = responseBody.indexOf("\"error\"");
                    if (errorIndex > 0) {
                        int start = responseBody.indexOf("\"", errorIndex + 7) + 1;
                        int end = responseBody.indexOf("\"", start);
                        if (end > start) {
                            errorMessage = "Shodan: " + responseBody.substring(start, end);
                        }
                    }
                } else {
                    // Use raw response body if it's not JSON
                    errorMessage = "Shodan: " + responseBody;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse Shodan error response", e);
        }
        
        // Handle specific HTTP status codes
        int statusCode = ex.getStatusCode().value();
        if (statusCode == 401) {
            errorMessage = "Shodan API key is invalid or unauthorized";
        } else if (statusCode == 403) {
            // Check if this is a domain endpoint error
            if (resource != null && !IP_PATTERN.matcher(resource).matches()) {
                errorMessage = "Shodan DNS domain endpoint requires a paid API subscription. " +
                              "Your current API plan does not include access to this endpoint. " +
                              "Please upgrade your Shodan API plan or use IP address scanning instead.";
            } else {
                errorMessage = "Shodan API access forbidden. Check your API key permissions.";
            }
        } else if (statusCode == 404) {
            if (resource != null) {
                errorMessage = "Resource not found in Shodan database: " + resource;
            } else {
                errorMessage = "Resource not found in Shodan database";
            }
        } else if (statusCode == 429) {
            errorMessage = "Shodan API rate limit exceeded. Please try again later.";
        }
        
        return errorMessage;
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}