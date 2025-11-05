package osint.service;

import osint.model.*;
import osint.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.Optional;

@Service
public class ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);

    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    private final GeminiService geminiService;
    private final ZapService zapService;

    private final EntityRepository entityRepository;
    private final ScanRepository scanRepository;
    private final ScanTargetRepository scanTargetRepository;
    private final ScanProviderRepository scanProviderRepository;
    private final ScanResultRepository scanResultRepository;
    private final ScanLogRepository scanLogRepository;

    // In-memory cache for quick status lookups (still used for real-time status)
    private final Map<String, ScanStatus> scanStatuses = new ConcurrentHashMap<>();

    @Autowired
    public ScanService(
            ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService hibpService,
            GeminiService geminiService,
            ZapService zapService,
            EntityRepository entityRepository,
            ScanRepository scanRepository,
            ScanTargetRepository scanTargetRepository,
            ScanProviderRepository scanProviderRepository,
            ScanResultRepository scanResultRepository,
            ScanLogRepository scanLogRepository) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
        this.geminiService = geminiService;
        this.zapService = zapService;
        this.entityRepository = entityRepository;
        this.scanRepository = scanRepository;
        this.scanTargetRepository = scanTargetRepository;
        this.scanProviderRepository = scanProviderRepository;
        this.scanResultRepository = scanResultRepository;
        this.scanLogRepository = scanLogRepository;
    }

    @Transactional
    public String startScan(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();
        logger.info("Starting scan: {} for targets: {}", scanId, request.getTargets());

        try {
            // Create Scan entity and save to DB
            Scan scan = new Scan(scanId, request.getType(), "RUNNING");
            scan.setName(request.getName());
            scan.setStartedAt(LocalDateTime.now());
            scan.setPriority("NORMAL");
            // Note: user_id is set to null for now (authentication context not available)
            // In production, get from SecurityContext: SecurityContextHolder.getContext().getAuthentication()
            
            scan = scanRepository.save(scan);

        // Create ScanTarget entities
        for (String target : request.getTargets()) {
            ScanTarget scanTarget = new ScanTarget(scan, target, request.getType());
            scanTargetRepository.save(scanTarget);
        }

        // Create ScanProvider entities
        for (String provider : request.getProviders()) {
            ScanProvider scanProvider = new ScanProvider(scan, provider);
            scanProvider.setStatus("PENDING");
            scanProviderRepository.save(scanProvider);
        }

        // Create in-memory status for API compatibility
        ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
        scanStatuses.put(scanId, status);

        // Log scan start
        addLogToDB(scan.getId(), "INFO", "Scan started: " + request.getName());

            // Execute async with DB persistence
            executeScanAsyncWithDB(scan.getId(), scanId, request);

            return scanId;
        } catch (Exception e) {
            logger.error("Error starting scan (DB may not have scan tables yet): {}", e.getMessage(), e);
            // Fallback: still create in-memory status for API compatibility
            ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
            scanStatuses.put(scanId, status);
            // Try to execute without DB persistence as fallback
            executeScanAsyncWithoutDB(scanId, request);
            return scanId;
        }
    }

    // Fallback method for when DB tables don't exist yet
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
            results.put("name", request.getName());
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

                        if (providerUpper.equals("HAVEIBEENPWNED")) {
                            providerUpper = "HIBP";
                        }

                        switch (providerUpper) {
                            case "SHODAN":
                                if (request.getType().equals("ip")) {
                                    providerResult = shodanService.getHostInfo(target).block();
                                } else if (request.getType().equals("domain")) {
                                    providerResult = shodanService.getDomainInfo(target).block();
                                }
                                break;
                            case "VIRUSTOTAL":
                                if (request.getType().equals("ip")) {
                                    providerResult = virusTotalService.getIpReport(target).block();
                                } else if (request.getType().equals("domain")) {
                                    providerResult = virusTotalService.getDomainReport(target).block();
                                }
                                break;
                            case "HIBP":
                                if (request.getType().equals("email")) {
                                    providerResult = hibpService.checkEmailBreach(target).block();
                                }
                                break;
                            case "ZAP":
                                if (request.getType().equals("url")) {
                                    providerResult = zapService.scanUrl(target).block();
                                }
                                break;
                            default:
                                logger.warn("Unknown provider: '{}'", provider);
                                providerResult = Map.of("error", "Provider not supported: " + provider);
                                break;
                        }

                        if (providerResult != null && !providerResult.containsKey("error")) {
                            providerResult.put("provider", provider);
                            targetResult.put(provider, providerResult);
                        } else if (providerResult != null) {
                            targetResult.put(provider, providerResult);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing provider {}: {}", provider, e.getMessage());
                        targetResult.put(provider, Map.of("error", e.getMessage()));
                    }
                }

                providerResults.put(target, targetResult);
            }

            results.put("results", providerResults);
            results.put("data", providerResults);
            results.put("gemini_reports", new HashMap<>());

            status.setStatus("COMPLETED");
            status.setResults(results);
            status.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error executing scan {}: {}", scanId, e.getMessage(), e);
            status.setStatus("FAILED");
            status.setErrorMessage(e.getMessage());
        }
    }

    @Async("taskExecutor")
    @Transactional
    private void executeScanAsyncWithDB(Long scanDbId, String scanId, ScanRequest request) {
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
            results.put("name", request.getName());
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

                        // Normalize provider names (frontend sends "VirusTotal", "Shodan",
                        // "HaveIBeenPwned")
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
                            case "ZAP":
                                logger.info("Matched ZAP for type: {}", request.getType());
                                if (request.getType().equals("url")) {
                                    providerResult = zapService.scanUrl(target).block();
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
                        logger.error("Error processing provider {} for target {}: {}", provider, target, e.getMessage(),
                                e);
                        targetResult.put(provider, Map.of("error", e.getMessage()));
                    }
                }

                providerResults.put(target, targetResult);
            }

            results.put("results", providerResults);
            results.put("data", providerResults); // Also add as 'data' for frontend compatibility

            // Initialize Gemini reports map
            results.put("gemini_reports", new HashMap<>());

            // Load scan from DB
            Optional<Scan> scanOpt = scanRepository.findById(scanDbId);
            if (!scanOpt.isPresent()) {
                logger.error("Scan not found in DB: {}", scanDbId);
                status.setStatus("FAILED");
                status.setErrorMessage("Scan not found in database");
                return;
            }
            Scan scan = scanOpt.get();

            // Get scan targets and providers from DB
            List<ScanTarget> scanTargets = scanTargetRepository.findByScanId(scanDbId);
            List<ScanProvider> scanProviders = scanProviderRepository.findByScanId(scanDbId);

            // Save results to DB for each target-provider combination
            Map<String, Long> scanResultIds = new HashMap<>();
            for (ScanTarget scanTarget : scanTargets) {
                String target = scanTarget.getTarget();
                Object targetResultObj = providerResults.get(target);
                if (targetResultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> targetResult = (Map<String, Object>) targetResultObj;
                    
                    for (ScanProvider scanProvider : scanProviders) {
                        String provider = scanProvider.getProviderName();
                        Object providerData = targetResult.get(provider);
                        
                        if (providerData instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> providerResult = (Map<String, Object>) providerData;
                            
                            // Create ScanResult
                            ScanResult scanResult = new ScanResult(scan, provider, providerResult);
                            scanResult.setScanTarget(scanTarget);
                            
                            // Calculate findings count
                            int findingsCount = calculateFindingsCount(providerResult);
                            scanResult.setFindingsCount(findingsCount);
                            
                            // Save to DB
                            scanResult = scanResultRepository.save(scanResult);
                            scanResultIds.put(provider + "_" + target, scanResult.getId());
                            
                            // Update provider status
                            scanProvider.setStatus(providerResult.containsKey("error") ? "FAILED" : "COMPLETED");
                            scanProvider.setCompletedAt(LocalDateTime.now());
                            if (providerResult.containsKey("error")) {
                                scanProvider.setErrorMessage(providerResult.get("error").toString());
                            }
                            scanProviderRepository.save(scanProvider);
                            
                            // Update target status
                            scanTarget.setStatus("COMPLETED");
                            scanTarget.setProcessedAt(LocalDateTime.now());
                            scanTargetRepository.save(scanTarget);
                            
                            // Generate Gemini report asynchronously if no error
                            if (!providerResult.containsKey("error")) {
                                generateGeminiReportAsync(scanId, scanResult.getId(), provider, target, providerResult,
                                        request.getType());
                            }
                        }
                    }
                }
            }

            // Update scan status to COMPLETED
            scan.setStatus("COMPLETED");
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);

            // Update in-memory status
            status.setStatus("COMPLETED");
            status.setResults(results);
            status.setCompletedAt(LocalDateTime.now());

            addLogToDB(scanDbId, "INFO", "Scan completed successfully");

        } catch (Exception e) {
            logger.error("Error executing scan {}: {}", scanId, e.getMessage(), e);
            
            // Update scan status to FAILED in DB
            Optional<Scan> scanOpt = scanRepository.findById(scanDbId);
            if (scanOpt.isPresent()) {
                Scan scan = scanOpt.get();
                scan.setStatus("FAILED");
                scan.setErrorMessage(e.getMessage());
                scan.setCompletedAt(LocalDateTime.now());
                scanRepository.save(scan);
                addLogToDB(scanDbId, "ERROR", "Scan failed: " + e.getMessage());
            }
            
            status.setStatus("FAILED");
            status.setErrorMessage(e.getMessage());
        }
    }

    private int calculateFindingsCount(Map<String, Object> providerResult) {
        // Simple heuristic: count non-null, non-error keys
        int count = 0;
        for (Object value : providerResult.values()) {
            if (value != null && !value.equals("error")) {
                if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                    count++;
                } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                    count += ((List<?>) value).size();
                } else if (!(value instanceof String && value.equals("error"))) {
                    count++;
                }
            }
        }
        return count;
    }

    // Old executeScanAsync method removed - using executeScanAsyncWithoutDB instead
    // (Scan tables don't exist in current DB schema)

    /**
     * Generate Gemini report asynchronously for a specific provider-target
     * combination and save to DB
     */
    @Async("taskExecutor")
    @Transactional
    private void generateGeminiReportAsync(String scanIdStr, Long scanResultId, String provider, String target,
            Map<String, Object> resultData, String scanType) {
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

                                    final String successLogMsg = "✓ AI analysis completed for " + provider + " - "
                                            + target;
                                    logger.info("[Scan {}] {}", scanIdStr, successLogMsg);

                                    // Save Gemini report to DB
                                    if (scanResultId != null) {
                                        Optional<ScanResult> scanResultOpt = scanResultRepository.findById(scanResultId);
                                        if (scanResultOpt.isPresent()) {
                                            ScanResult scanResult = scanResultOpt.get();
                                            scanResult.setGeminiReport(geminiAnalysis);
                                            scanResultRepository.save(scanResult);
                                        }
                                    }

                                    // Update in-memory status
                                    if (status != null) {
                                        addLog(status, successLogMsg);
                                        if (status.getResult() != null) {
                                            Map<String, Object> geminiReports = (Map<String, Object>) status.getResult()
                                                    .getOrDefault("gemini_reports", new HashMap<>());
                                            geminiReports.put(provider + "_" + target, geminiAnalysis);
                                            status.getResult().put("gemini_reports", geminiReports);
                                        }
                                    }
                                } else {
                                    final String errorMsg = "⚠ AI analysis failed for " + provider + " - " + target
                                            + ": " +
                                            (geminiAnalysis != null ? geminiAnalysis.get("error") : "Unknown error");
                                    logger.warn("[Scan {}] {}", scanIdStr, errorMsg);
                                    if (status != null) {
                                        addLog(status, errorMsg);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Error processing Gemini analysis result for " + provider + " - " + target,
                                        e);
                                handleGeminiError(scanResultId, scanIdStr, provider, target, e.getMessage());
                            }
                        },
                        error -> {
                            logger.error("Error generating Gemini analysis for " + provider + " - " + target, error);
                            handleGeminiError(scanResultId, scanIdStr, provider, target, error.getMessage());
                        });
    }

    // Repository-dependent methods removed - Scan tables don't exist in current DB
    // schema
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

    private void handleGeminiError(Long scanResultId, String scanIdStr, String provider, String target,
            String errorMessage) {
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

    /**
     * Delete scan from database and memory
     */
    @Transactional
    public void deleteScan(String scanId) {
        try {
            // Find scan in DB
            Optional<Scan> scanOpt = scanRepository.findByScanId(scanId);
            if (scanOpt.isPresent()) {
                Scan scan = scanOpt.get();
                // Delete from DB (cascade will delete related records)
                scanRepository.delete(scan);
                logger.info("Scan {} deleted from database", scanId);
            }
            
            // Remove from in-memory cache
            scanStatuses.remove(scanId);
            logger.info("Scan {} removed from memory cache", scanId);
        } catch (Exception e) {
            logger.error("Error deleting scan {}: {}", scanId, e.getMessage(), e);
            throw new RuntimeException("Scan silinemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Get all scans from database
     * Returns a list of scan summaries for history display
     */
    public List<Map<String, Object>> getAllScans() {
        List<Map<String, Object>> scanList = new ArrayList<>();
        
        try {
            // Get all scans from DB, ordered by creation date descending
            List<Scan> scans = scanRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
            );
            
            logger.debug("Found {} scans in database", scans.size());
        
        for (Scan scan : scans) {
            Map<String, Object> scanSummary = new HashMap<>();
            scanSummary.put("scanId", scan.getScanId());
            scanSummary.put("name", scan.getName());
            scanSummary.put("type", scan.getType());
            scanSummary.put("status", scan.getStatus().toLowerCase());
            scanSummary.put("createdAt", scan.getCreatedAt());
            scanSummary.put("startedAt", scan.getStartedAt());
            scanSummary.put("completedAt", scan.getCompletedAt());
            scanSummary.put("errorMessage", scan.getErrorMessage());
            
            // Get targets
            List<ScanTarget> targets = scanTargetRepository.findByScanId(scan.getId());
            List<String> targetList = new ArrayList<>();
            for (ScanTarget target : targets) {
                targetList.add(target.getTarget());
            }
            scanSummary.put("targets", targetList);
            
            // Get providers
            List<ScanProvider> providers = scanProviderRepository.findByScanId(scan.getId());
            List<String> providerList = new ArrayList<>();
            for (ScanProvider provider : providers) {
                providerList.add(provider.getProviderName());
            }
            scanSummary.put("providers", providerList);
            
            // Calculate findings count from scan results
            List<ScanResult> results = scanResultRepository.findByScanId(scan.getId());
            int findingsCount = 0;
            for (ScanResult result : results) {
                if (result.getFindingsCount() != null) {
                    findingsCount += result.getFindingsCount();
                }
            }
            scanSummary.put("findings", findingsCount);
            
            // Convert timestamp
            if (scan.getCreatedAt() != null) {
                scanSummary.put("timestamp", scan.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli());
            }
            
            scanList.add(scanSummary);
        }
        
        } catch (Exception e) {
            logger.error("Error getting scans from database: {}", e.getMessage(), e);
            // Return empty list instead of throwing exception
            // This allows the frontend to show the page even if DB has issues
            return scanList;
        }
        
        return scanList;
    }

    private void addLog(ScanStatus status, String message) {
        status.getLogs().add(new LogEntry(System.currentTimeMillis(), message));
        logger.info("[Scan {}] {}", status.getScanId(), message);
    }

    private void addLogToDB(Long scanDbId, String level, String message) {
        try {
            Optional<Scan> scanOpt = scanRepository.findById(scanDbId);
            if (scanOpt.isPresent()) {
                Scan scan = scanOpt.get();
                ScanLog scanLog = new ScanLog(scan, level, message);
                scanLogRepository.save(scanLog);
            }
        } catch (Exception e) {
            logger.error("Error saving log to DB: {}", e.getMessage());
        }
        logger.info("[Scan DB ID: {}] {}", scanDbId, message);
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

    public static class ScanServiceResult {
        private String scanId;
        private Map<String, Object> data;

        public ScanServiceResult(String scanId, Map<String, Object> data) {
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
