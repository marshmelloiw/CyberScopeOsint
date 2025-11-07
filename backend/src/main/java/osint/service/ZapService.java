package osint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.TimeoutException;

@Service
public class ZapService {

    private static final Logger logger = LoggerFactory.getLogger(ZapService.class);
    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final Duration spiderTimeout;
    private final Duration activeScanTimeout;
    private final Duration pollInterval;
    private final Duration startupTimeout;
    private final boolean autoStartEnabled;
    private final String daemonCommand;
    private final String daemonWorkingDirectory;
    private final int maxBodySizeBytes;

    public ZapService(@Value("${osint.zap.api-key:}") String apiKey,
                      @Value("${osint.zap.base-url:http://localhost:8081}") String baseUrl,
                      @Value("${osint.zap.spider-timeout-seconds:600}") long spiderTimeoutSeconds,
                      @Value("${osint.zap.active-timeout-seconds:1800}") long activeTimeoutSeconds,
                      @Value("${osint.zap.poll-interval-seconds:2}") long pollIntervalSeconds,
                      @Value("${osint.zap.startup-timeout-seconds:120}") long startupTimeoutSeconds,
                      @Value("${osint.zap.auto-start:false}") boolean autoStartEnabled,
                      @Value("${osint.zap.daemon-command:}") String daemonCommand,
                      @Value("${osint.zap.daemon-working-directory:}") String daemonWorkingDirectory,
                      @Value("${osint.zap.max-body-size-bytes:2097152}") int maxBodySizeBytes) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.spiderTimeout = Duration.ofSeconds(Math.max(spiderTimeoutSeconds, 30));
        this.activeScanTimeout = Duration.ofSeconds(Math.max(activeTimeoutSeconds, 60));
        this.pollInterval = Duration.ofSeconds(Math.max(pollIntervalSeconds, 1));
        this.startupTimeout = Duration.ofSeconds(Math.max(startupTimeoutSeconds, 10));
        this.autoStartEnabled = autoStartEnabled;
        this.daemonCommand = daemonCommand;
        this.daemonWorkingDirectory = daemonWorkingDirectory;
        this.maxBodySizeBytes = Math.max(maxBodySizeBytes, 262144);
        logger.info("ZapService - API Key: {}", (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));
        logger.info("ZapService - Base URL: {}", baseUrl);
        logger.info("ZapService - Spider timeout: {} seconds", this.spiderTimeout.toSeconds());
        logger.info("ZapService - Active scan timeout: {} seconds", this.activeScanTimeout.toSeconds());
        logger.info("ZapService - Poll interval: {} seconds", this.pollInterval.toSeconds());
        if (autoStartEnabled) {
            logger.info("ZapService - Auto-start is enabled");
        }

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(this.maxBodySizeBytes));

        // Sadece API key varsa header ekle
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("X-ZAP-API-Key", apiKey);
        }

        this.webClient = builder.build();

        if (this.autoStartEnabled) {
            ensureZapDaemonRunning();
        }
    }

    public Mono<Map<String, Object>> scanUrl(String url) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        return webClient.get()
                .uri(uriBuilder -> addApiKey(uriBuilder
                        .path("/JSON/spider/action/scan/")
                        .queryParam("url", url)).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to scan URL with ZAP"));
    }

    public Mono<Map<String, Object>> getSpiderStatus(String scanId) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        return webClient.get()
                .uri(uriBuilder -> addApiKey(uriBuilder
                        .path("/JSON/spider/view/status/")
                        .queryParam("scanId", scanId)).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to get spider status from ZAP"));
    }

    public Mono<Map<String, Object>> getActiveScanResults(String scanId) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        return webClient.get()
                .uri(uriBuilder -> addApiKey(uriBuilder
                        .path("/JSON/ascan/view/status/")
                        .queryParam("scanId", scanId)).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to get active scan results from ZAP"));
    }

    public Mono<Map<String, Object>> getAlerts(String baseUrl) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        String normalizedBaseUrl = normalizeBaseUrlForAlerts(baseUrl);

        return webClient.get()
                .uri(uriBuilder -> addApiKey(uriBuilder
                        .path("/JSON/core/view/alerts/")
                        .queryParam("start", 0)
                        .queryParam("count", 9999)).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> buildAlertsResponse((Map<String, Object>) response, normalizedBaseUrl))
                .onErrorResume(ex -> {
                    logger.error("Failed to get alerts from ZAP for {}", normalizedBaseUrl, ex);
                    return Mono.just(createErrorMap("Failed to get alerts from ZAP: " + ex.getMessage()));
                });
    }

    public Mono<Map<String, Object>> startActiveScan(String url) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        return webClient.get()
                .uri(uriBuilder -> addApiKey(uriBuilder
                        .path("/JSON/ascan/action/scan/")
                        .queryParam("url", url)
                        .queryParam("recurse", true)
                        .queryParam("scanpolicyname", "")
                        .queryParam("method", "")
                        .queryParam("postData", "")
                        .queryParam("contextId", "")).build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .onErrorReturn(createErrorMap("Failed to start active scan with ZAP"));
    }

    public Mono<Map<String, Object>> performFullScan(String originalUrl) {
        if (apiKey.isEmpty()) {
            return Mono.just(createErrorMap("ZAP API key not configured"));
        }

        if (autoStartEnabled) {
            ensureZapDaemonRunning();
        }

        String url = normalizeUrl(originalUrl);
        logger.info("Starting full ZAP scan for {}", url);

        return scanUrl(url)
                .flatMap(spiderResponse -> {
                    if (containsError(spiderResponse)) {
                        return Mono.just(spiderResponse);
                    }

                    String spiderId = Optional.ofNullable(spiderResponse.get("scan"))
                            .map(Object::toString)
                            .orElse("");

                    if (spiderId.isBlank()) {
                        return Mono.just(createErrorMap("ZAP spider did not return a scan id"));
                    }

                    return waitForSpiderCompletion(spiderId)
                            .then(startActiveScan(url))
                            .flatMap(activeScanResponse -> {
                                if (containsError(activeScanResponse)) {
                                    return Mono.just(activeScanResponse);
                                }

                                String activeScanId = Optional.ofNullable(activeScanResponse.get("scan"))
                                        .map(Object::toString)
                                        .orElse("");

                                if (activeScanId.isBlank()) {
                                    return Mono.just(createErrorMap("ZAP active scan did not return a scan id"));
                                }

                                return waitForActiveScanCompletion(activeScanId)
                                        .then(getAlerts(url))
                                        .map(alertsResponse -> buildResultMap(url, spiderResponse, activeScanResponse, alertsResponse));
                            })
                            .timeout(activeScanTimeout.plus(spiderTimeout))
                            .onErrorResume(TimeoutException.class, ex -> {
                                logger.warn("ZAP scan timed out for {}", url);
                                return Mono.just(createErrorMap("ZAP scan timed out"));
                            });
                })
                .timeout(spiderTimeout.plus(activeScanTimeout))
                .onErrorResume(TimeoutException.class, ex -> {
                    logger.warn("ZAP scan timed out for {}", url);
                    return Mono.just(createErrorMap("ZAP scan timed out"));
                })
                .onErrorResume(ex -> {
                    logger.error("ZAP scan failed for {}", url, ex);
                    return Mono.just(createErrorMap("ZAP scan failed: " + ex.getMessage()));
                });
    }

    private Mono<Void> waitForSpiderCompletion(String scanId) {
        return Flux.interval(Duration.ZERO, pollInterval)
                .flatMap(tick -> getSpiderStatus(scanId))
                .flatMap(response -> {
                    if (containsError(response)) {
                        return Mono.error(new IllegalStateException(String.valueOf(response.get("error"))));
                    }

                    int status = parseStatus(response);
                    logger.debug("ZAP spider progress for {}: {}%", scanId, status);
                    if (status >= 100) {
                        return Mono.just(status);
                    }
                    return Mono.empty();
                })
                .next()
                .timeout(spiderTimeout)
                .then();
    }

    private Mono<Void> waitForActiveScanCompletion(String scanId) {
        return Flux.interval(Duration.ZERO, pollInterval)
                .flatMap(tick -> getActiveScanResults(scanId))
                .flatMap(response -> {
                    if (containsError(response)) {
                        return Mono.error(new IllegalStateException(String.valueOf(response.get("error"))));
                    }

                    int status = parseStatus(response);
                    logger.debug("ZAP active scan progress for {}: {}%", scanId, status);
                    if (status >= 100) {
                        return Mono.just(status);
                    }
                    return Mono.empty();
                })
                .next()
                .timeout(activeScanTimeout)
                .then();
    }

    private Map<String, Object> buildResultMap(String targetUrl,
                                               Map<String, Object> spiderResponse,
                                               Map<String, Object> activeScanResponse,
                                               Map<String, Object> alertsResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("target", targetUrl);
        result.put("spider", spiderResponse);
        result.put("activeScan", activeScanResponse);
        result.put("alerts_raw", alertsResponse.getOrDefault("alerts_raw", alertsResponse));
        result.put("alerts", alertsResponse.getOrDefault("alerts", alertsResponse));
        result.put("provider", "ZAP");
        return result;
    }

    private boolean containsError(Map<String, Object> response) {
        return response != null && response.containsKey("error");
    }

    private int parseStatus(Map<String, Object> response) {
        Object statusValue = response.get("status");
        if (statusValue == null) {
            return 0;
        }
        try {
            return Integer.parseInt(statusValue.toString());
        } catch (NumberFormatException ex) {
            logger.warn("Unexpected ZAP status payload: {}", statusValue);
            return 0;
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private synchronized void ensureZapDaemonRunning() {
        if (!autoStartEnabled) {
            return;
        }

        if (isZapResponsive()) {
            return;
        }

        if (daemonCommand == null || daemonCommand.isBlank()) {
            logger.warn("ZAP auto-start is enabled but no daemon command is configured");
            return;
        }

        logger.info("ZAP daemon not reachable. Attempting to start using command: {}", daemonCommand);

        try {
            startZapDaemonProcess();
            waitForZapStartup();
        } catch (IOException ex) {
            logger.error("Failed to start ZAP daemon process", ex);
        }
    }

    private boolean isZapResponsive() {
        try {
            webClient.get()
                    .uri(uriBuilder -> addApiKey(uriBuilder
                            .path("/JSON/core/view/version/")).build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception ex) {
            logger.debug("ZAP daemon not yet responsive at {}: {}", baseUrl, ex.getMessage());
            return false;
        }
    }

    private void startZapDaemonProcess() throws IOException {
        List<String> commandParts = tokenizeCommand(daemonCommand);
        if (commandParts.isEmpty()) {
            throw new IOException("ZAP daemon command is empty after tokenization");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        if (daemonWorkingDirectory != null && !daemonWorkingDirectory.isBlank()) {
            processBuilder.directory(new File(daemonWorkingDirectory));
        }
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = processBuilder.start();
        if (!process.isAlive()) {
            try {
                int exitCode = process.waitFor();
                throw new IOException("ZAP daemon process exited immediately with code " + exitCode);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for ZAP daemon process", ex);
            }
        }
        logger.info("ZAP daemon process started (PID: {})", process.pid());
    }

    private void waitForZapStartup() {
        Instant deadline = Instant.now().plus(startupTimeout);
        while (Instant.now().isBefore(deadline)) {
            if (isZapResponsive()) {
                logger.info("ZAP daemon is now reachable at {}", baseUrl);
                return;
            }
            try {
                Thread.sleep(Math.min(pollInterval.toMillis(), 2000));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for ZAP daemon to start");
                return;
            }
        }
        logger.warn("Timed out after {} seconds waiting for ZAP daemon to become reachable at {}",
                startupTimeout.toSeconds(), baseUrl);
    }

    private UriBuilder addApiKey(UriBuilder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.queryParam("apikey", apiKey);
        }
        return builder;
    }

    private List<String> tokenizeCommand(String commandLine) {
        if (commandLine == null) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\0';

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if (c == '\"' || c == '\'') {
                if (inQuotes && quoteChar == c) {
                    inQuotes = false;
                    quoteChar = '\0';
                } else if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private String normalizeBaseUrlForAlerts(String url) {
        if (url == null) {
            return null;
        }

        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private Map<String, Object> buildAlertsResponse(Map<String, Object> response, String targetBaseUrl) {
        List<Map<String, Object>> allAlerts = extractAlerts(response);
        List<Map<String, Object>> filteredAlerts = filterAlertsByBaseUrl(allAlerts, targetBaseUrl);

        Map<String, Object> result = new HashMap<>();
        if (response != null) {
            result.putAll(response);
        }
        result.put("alerts_raw", allAlerts);
        result.put("alerts", filteredAlerts);
        result.put("alerts_count", filteredAlerts.size());
        result.put("alerts_total", allAlerts.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAlerts(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }

        Object alertsObj = response.get("alerts");
        if (alertsObj instanceof List<?>) {
            return ((List<?>) alertsObj).stream()
                    .filter(Map.class::isInstance)
                    .map(alert -> (Map<String, Object>) alert)
                    .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    private List<Map<String, Object>> filterAlertsByBaseUrl(List<Map<String, Object>> alerts, String targetBaseUrl) {
        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            return alerts;
        }

        String normalizedTarget = normalizeBaseUrlForAlerts(targetBaseUrl);
        return alerts.stream()
                .filter(alert -> alertMatchesBaseUrl(alert, normalizedTarget))
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean alertMatchesBaseUrl(Map<String, Object> alert, String normalizedTarget) {
        if (alert == null || normalizedTarget == null) {
            return false;
        }

        Object alertUrlObj = alert.get("url");
        if (alertUrlObj instanceof String) {
            String alertUrl = (String) alertUrlObj;
            return alertUrl.startsWith(normalizedTarget);
        }

        Object evidenceUrlObj = alert.get("evidence");
        if (evidenceUrlObj instanceof String) {
            String evidenceUrl = ((String) evidenceUrlObj).trim();
            if (evidenceUrl.startsWith(normalizedTarget)) {
                return true;
            }
        }

        Object otherInfoObj = alert.get("other");
        if (otherInfoObj instanceof String) {
            String otherInfo = (String) otherInfoObj;
            return otherInfo.contains(normalizedTarget);
        }

        return false;
    }
}
