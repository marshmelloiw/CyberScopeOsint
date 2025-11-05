package osint.service;

import osint.model.Entity;
import osint.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

@Service
public class ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);

    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    private final GeminiService geminiService;

    // Note: Scan tables don't exist in current DB schema
    // All scan operations are in-memory only
    private final EntityRepository entityRepository;

    // In-memory cache for quick status lookups
    private final Map<String, ScanStatus> scanStatuses = new ConcurrentHashMap<>();

    @Autowired
    public ScanService(
            ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService hibpService,
            GeminiService geminiService,
            EntityRepository entityRepository) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
        this.geminiService = geminiService;
        this.entityRepository = entityRepository;
    }

    public String startScan(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();

        // Note: Scan tables (scans, scan_targets, scan_providers) don't exist in current DB schema
        // Using in-memory storage only for now
        logger.info("Starting scan: {} for targets: {}", scanId, request.getTargets());

        // Create in-memory status for API compatibility
        ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
        scanStatuses.put(scanId, status);

        // Execute async without DB persistence
        executeScanAsyncWithoutDB(scanId, request);

        return scanId;
    }
    
    @Async("taskExecutor")
    private void executeScanAsyncWithoutDB(String scanId, ScanRequest request) {
        ScanStatus status = scanStatuses.get(scanId);
        if (status == null) {
            status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
            scanStatuses.put(scanId, status);
        }

        try {
            Map<String, Object> results = new HashMap<>();
            results.put("scanId", scanId);
            results.put("type", request.getType());
            results.put("targets", request.getTargets());
            results.put("providers", request.getProviders());
            results.put("timestamp", System.currentTimeMillis());

            Map<String, Object> providerResults = new HashMap<>();
            
            // Process each target
            for (String target : request.getTargets()) {
                Map<String, Object> targetResult = new HashMap<>();
                
                // Process each provider
                for (String provider : request.getProviders()) {
                    try {
                        Map<String, Object> providerResult = new HashMap<>();
                        String providerUpper = provider.toUpperCase(Locale.ENGLISH).trim();
                        
                        logger.info("Processing provider: '{}' (normalized: '{}')", provider, providerUpper);
                        
                        // Normalize provider names (frontend sends "VirusTotal", "Shodan", "HaveIBeenPwned")
                        if (providerUpper.equals("HAVEIBEENPWNED")) {
                            providerUpper = "HIBP";
                        }
                        
                        switch (providerUpper) {
                            case "SHODAN":
                                logger.info("Matched SHODAN for type: {}", request.getType());
                                if (request.getType().equals("ip")) {
                                    providerResult = shodanService.getHostInfo(target).block();
                                } else if (request.getType().equals("domain")) {
                                    providerResult = shodanService.getDomainInfo(target).block();
                                }
                                break;
                            case "VIRUSTOTAL":
                                logger.info("Matched VIRUSTOTAL for type: {}", request.getType());
                                if (request.getType().equals("ip")) {
                                    providerResult = virusTotalService.getIpReport(target).block();
                                } else if (request.getType().equals("domain")) {
                                    providerResult = virusTotalService.getDomainReport(target).block();
                                }
                                break;
                            case "HIBP":
                                logger.info("Matched HIBP for type: {}", request.getType());
                                if (request.getType().equals("email")) {
                                    providerResult = hibpService.checkEmailBreach(target).block();
                                }
                                break;
                            default:
                                logger.warn("Unknown provider: '{}' (normalized: '{}')", provider, providerUpper);
                                providerResult = Map.of("error", "Provider not supported: " + provider);
                                break;
                        }
                        
                        if (providerResult != null && !providerResult.containsKey("error")) {
                            providerResult.put("provider", provider); // Keep original name for frontend
                            targetResult.put(provider, providerResult);
                        } else if (providerResult != null) {
                            targetResult.put(provider, providerResult);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing provider {} for target {}: {}", provider, target, e.getMessage(), e);
                        targetResult.put(provider, Map.of("error", e.getMessage()));
                    }
                }
                
                providerResults.put(target, targetResult);
            }
            
            results.put("results", providerResults);
            results.put("data", providerResults); // Also add as 'data' for frontend compatibility
            
            status.setStatus("COMPLETED");
            status.setResults(results);
            status.setCompletedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error executing scan {}: {}", scanId, e.getMessage(), e);
            status.setStatus("FAILED");
            status.setErrorMessage(e.getMessage());
        }
    }

    // Old executeScanAsync method removed - using executeScanAsyncWithoutDB instead
    // (Scan tables don't exist in current DB schema)

    /**
     * Generate Gemini report asynchronously for a specific provider-target combination
     * Note: Scan tables don't exist, so reports are stored in memory only
     */
    @Async("taskExecutor")
    private void generateGeminiReportAsync(String scanIdStr, Long scanResultId, String provider, String target, Map<String, Object> resultData, String scanType) {
        // Log start
        final String initialLogMsg = "Generating AI analysis for " + provider + " - " + target + "...";
        logger.info("[Scan {}] {}", scanIdStr, initialLogMsg);
        
        ScanStatus status = scanStatuses.get(scanIdStr);
        if (status != null) {
            addLog(status, initialLogMsg);
        }
        
        // Prepare data for this specific provider-target
        Map<String, Object> providerData = new HashMap<>();
        providerData.put(provider + "_" + target, resultData);
        
        // Generate report asynchronously (non-blocking)
        geminiService.analyzeScanResults(providerData, scanType)
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                geminiAnalysis -> {
                    // Success callback
                    try {
                        if (geminiAnalysis != null && !geminiAnalysis.containsKey("error")) {
                            geminiAnalysis.put("provider", provider);
                            geminiAnalysis.put("target", target);
                            geminiAnalysis.put("status", "completed");
                            
                            final String successLogMsg = "✓ AI analysis completed for " + provider + " - " + target;
                            logger.info("[Scan {}] {}", scanIdStr, successLogMsg);
                            
                            // Update in-memory status
                            if (status != null) {
                                addLog(status, successLogMsg);
                                if (status.getResult() != null) {
                                    Map<String, Object> geminiReports = (Map<String, Object>) status.getResult().getOrDefault("gemini_reports", new HashMap<>());
                                    geminiReports.put(provider + "_" + target, geminiAnalysis);
                                    status.getResult().put("gemini_reports", geminiReports);
                                }
                            }
                        } else {
                            final String errorMsg = "⚠ AI analysis failed for " + provider + " - " + target + ": " + 
                                (geminiAnalysis != null ? geminiAnalysis.get("error") : "Unknown error");
                            logger.warn("[Scan {}] {}", scanIdStr, errorMsg);
                            if (status != null) {
                                addLog(status, errorMsg);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing Gemini analysis result for " + provider + " - " + target, e);
                        handleGeminiError(scanResultId, scanIdStr, provider, target, e.getMessage());
                    }
                },
                error -> {
                    logger.error("Error generating Gemini analysis for " + provider + " - " + target, error);
                    handleGeminiError(scanResultId, scanIdStr, provider, target, error.getMessage());
                }
            );
    }
    
    // Repository-dependent methods removed - Scan tables don't exist in current DB schema
    // Using in-memory operations only
    private void saveGeneratingStatus(Long scanResultId, String provider, String target) {
        // No-op - scan tables don't exist
    }
    
    private void saveCompletedReport(Long scanResultId, Map<String, Object> report) {
        // No-op - scan tables don't exist
    }
    
    private void saveFailedReport(Long scanResultId, Map<String, Object> report) {
        // No-op - scan tables don't exist
    }
    
    private void handleGeminiError(Long scanResultId, String scanIdStr, String provider, String target, String errorMessage) {
        String errorMsg = "⚠ AI analysis error for " + provider + " - " + target + ": " + errorMessage;
        logger.warn("[Scan {}] {}", scanIdStr, errorMsg);
        
        // Update in-memory status if exists
        ScanStatus status = scanStatuses.get(scanIdStr);
        if (status != null) {
            addLog(status, errorMsg);
        }
    }
    
    private void markScanCompleted(Long scanId, Map<String, Object> results) {
        // No-op - scan tables don't exist
    }
    
    private void addLogToDBAsync(String scanId, String level, String message) {
        // No-op - scan tables don't exist
        logger.info("[Scan {}] {}", scanId, message);
    }

    private void updateScanStatusCache(String scanId) {
        // No-op - using in-memory cache only
    }

    private ScanStatus getScanStatusFromDB(String scanId) {
        // No-op - scan tables don't exist, return null
        return null;
    }

    private Map<String, Object> buildScanResultMap(String scanId) {
        // No-op - scan tables don't exist
        return new HashMap<>();
    }

    public ScanStatus getScanStatus(String scanId) {
        // Return from in-memory cache only (DB tables don't exist)
        return scanStatuses.get(scanId);
    }

    private void addLog(ScanStatus status, String message) {
        status.getLogs().add(new LogEntry(System.currentTimeMillis(), message));
        logger.info("[Scan {}] {}", status.getScanId(), message);
    }

    private void addLogToDB(String scanId, String level, String message) {
        // No-op - scan tables don't exist
        logger.info("[Scan {}] {}", scanId, message);
    }

    private Entity updateOrCreateEntity(String entityType, String entityValue, Object scan) {
        // Update entity in DB (entities table exists)
        // Note: user_id is required, but we can't get it without authentication context
        // For now, skip entity creation to avoid errors
        try {
            Optional<Entity> entityOpt = entityRepository.findByEntityTypeAndEntityValue(entityType, entityValue);
            if (entityOpt.isPresent()) {
                Entity entity = entityOpt.get();
                entity.setLastScanAt(LocalDateTime.now());
                return entityRepository.save(entity);
            }
            // Don't create new entity without user_id
            return null;
        } catch (Exception e) {
            logger.error("Error updating entity: {}", e.getMessage());
            return null;
        }
    }

    // Inner classes
    public static class ScanRequest {
        private String type;
        private List<String> targets;
        private List<String> providers;
        private String name;

        public ScanRequest() {
        }

        public ScanRequest(String type, List<String> targets, List<String> providers, String name) {
            this.type = type;
            this.targets = targets;
            this.providers = providers;
            this.name = name;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getTargets() {
            return targets;
        }

        public void setTargets(List<String> targets) {
            this.targets = targets;
        }

        public List<String> getProviders() {
            return providers;
        }

        public void setProviders(List<String> providers) {
            this.providers = providers;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ScanResult {
        private String scanId;
        private Map<String, Object> data;

        public ScanResult(String scanId, Map<String, Object> data) {
            this.scanId = scanId;
            this.data = data;
        }

        public String getScanId() {
            return scanId;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    public static class ScanStatus {
        private String scanId;
        private String status;
        private List<LogEntry> logs;
        private Map<String, Object> result;
        private LocalDateTime completedAt;
        private String errorMessage;

        public ScanStatus(String scanId, String status, List<LogEntry> logs, Map<String, Object> result) {
            this.scanId = scanId;
            this.status = status;
            this.logs = logs;
            this.result = result;
        }

        // Getters and Setters
        public String getScanId() {
            return scanId;
        }

        public void setScanId(String scanId) {
            this.scanId = scanId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<LogEntry> getLogs() {
            return logs;
        }

        public void setLogs(List<LogEntry> logs) {
            this.logs = logs;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }
        
        public void setResults(Map<String, Object> results) {
            this.result = results;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class LogEntry {
        private long timestamp;
        private String message;

        public LogEntry(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }
    }
}
