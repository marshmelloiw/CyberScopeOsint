package osint.service;

import osint.model.*;
import osint.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);

    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;

    private final ScanRepository scanRepository;
    private final ScanTargetRepository scanTargetRepository;
    private final ScanProviderRepository scanProviderRepository;
    private final ScanResultRepository scanResultRepository;
    private final ScanLogRepository scanLogRepository;
    private final EntityRepository entityRepository;

    // In-memory cache for quick status lookups (optional - can be removed)
    private final Map<String, ScanStatus> scanStatuses = new ConcurrentHashMap<>();

    @Autowired
    public ScanService(ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService hibpService,
            ScanRepository scanRepository,
            ScanTargetRepository scanTargetRepository,
            ScanProviderRepository scanProviderRepository,
            ScanResultRepository scanResultRepository,
            ScanLogRepository scanLogRepository,
            EntityRepository entityRepository) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
        this.scanRepository = scanRepository;
        this.scanTargetRepository = scanTargetRepository;
        this.scanProviderRepository = scanProviderRepository;
        this.scanResultRepository = scanResultRepository;
        this.scanLogRepository = scanLogRepository;
        this.entityRepository = entityRepository;
    }

    @Transactional
    public String startScan(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();

        // Create Scan entity
        Scan scan = new Scan();
        scan.setScanId(scanId);
        scan.setName(request.getName());
        scan.setType(request.getType());
        scan.setStatus("RUNNING");
        scan.setPriority("NORMAL");
        scan.setStartedAt(LocalDateTime.now());
        scan = scanRepository.save(scan);

        // Create scan targets
        for (String target : request.getTargets()) {
            ScanTarget scanTarget = new ScanTarget(scan, target, request.getType());
            scanTargetRepository.save(scanTarget);
        }

        // Create scan providers
        for (String provider : request.getProviders()) {
            ScanProvider scanProvider = new ScanProvider(scan, provider);
            scanProviderRepository.save(scanProvider);
        }

        // Add initial log
        addLogToDB(scan, "INFO", "Initializing scan for: " + request.getType());
        addLogToDB(scan, "INFO", "Targets: " + String.join(", ", request.getTargets()));

        // Create in-memory status for API compatibility
        ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
        scanStatuses.put(scanId, status);

        // Execute async
        executeScanAsync(scanId, request);

        return scanId;
    }

    @Async("taskExecutor")
    @Transactional
    private void executeScanAsync(String scanId, ScanRequest request) {
        Optional<Scan> scanOpt = scanRepository.findByScanId(scanId);
        if (scanOpt.isEmpty()) {
            logger.error("Scan not found: {}", scanId);
            return;
        }

        Scan scan = scanOpt.get();
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
            List<ScanTarget> targets = scanTargetRepository.findByScanId(scan.getId());
            List<ScanProvider> providers = scanProviderRepository.findByScanId(scan.getId());

            // Process each target
            for (ScanTarget scanTarget : targets) {
                String target = scanTarget.getTarget();
                addLog(status, "Processing target: " + target);
                addLogToDB(scan, "INFO", "Processing target: " + target);

                scanTarget.setStatus("PROCESSING");
                scanTargetRepository.save(scanTarget);

                // Update or create entity (for tracking)
                updateOrCreateEntity(scanTarget.getTargetType(), target, scan);

                // Process each provider
                for (ScanProvider scanProvider : providers) {
                    String provider = scanProvider.getProviderName();
                    addLog(status, "Querying " + provider + " for " + target + "...");
                    addLogToDB(scan, "INFO", "Querying " + provider + " for " + target + "...");

                    scanProvider.setStatus("PROCESSING");
                    scanProviderRepository.save(scanProvider);

                    try {
                        String providerLower = provider.toLowerCase().trim();
                        Map<String, Object> result = null;

                        switch (providerLower) {
                            case "shodan":
                                if ("domain".equals(request.getType())) {
                                    result = shodanService.getDomainInfo(target).block();
                                    providerResults.put(provider + "_" + target, result);
                                } else if ("ip".equals(request.getType())) {
                                    result = shodanService.getHostInfo(target).block();
                                    providerResults.put(provider + "_" + target, result);
                                } else {
                                    addLog(status, "⚠ Shodan does not support scan type: " + request.getType());
                                    continue;
                                }

                                // Check for errors in result
                                if (result != null && result.containsKey("error")) {
                                    String errorMsg = "✗ " + provider + " error: " + result.get("error");
                                    addLog(status, errorMsg);
                                    addLogToDB(scan, "ERROR", errorMsg);
                                    scanProvider.setStatus("FAILED");
                                    scanProvider.setErrorMessage(String.valueOf(result.get("error")));
                                } else {
                                    String successMsg = "✓ " + provider + " query completed for " + target;
                                    addLog(status, successMsg);
                                    addLogToDB(scan, "INFO", successMsg);

                                    // Save result to DB
                                    saveScanResult(scan, scanTarget, scanProvider, result);

                                    scanProvider.setStatus("COMPLETED");
                                    scanProvider.setCompletedAt(LocalDateTime.now());
                                }
                                scanProviderRepository.save(scanProvider);
                                break;

                            case "virustotal":
                            case "vt":
                                if ("domain".equals(request.getType())) {
                                    result = virusTotalService.getDomainReport(target).block();
                                    providerResults.put(provider + "_" + target, result);
                                } else if ("ip".equals(request.getType())) {
                                    result = virusTotalService.getIpReport(target).block();
                                    providerResults.put(provider + "_" + target, result);
                                } else {
                                    addLog(status, "⚠ VirusTotal does not support scan type: " + request.getType());
                                    continue;
                                }

                                if (result != null && result.containsKey("error")) {
                                    String errorMsg = "✗ " + provider + " error: " + result.get("error");
                                    addLog(status, errorMsg);
                                    addLogToDB(scan, "ERROR", errorMsg);
                                    scanProvider.setStatus("FAILED");
                                    scanProvider.setErrorMessage(String.valueOf(result.get("error")));
                                } else {
                                    String successMsg = "✓ " + provider + " query completed for " + target;
                                    addLog(status, successMsg);
                                    addLogToDB(scan, "INFO", successMsg);

                                    // Save result to DB
                                    saveScanResult(scan, scanTarget, scanProvider, result);

                                    scanProvider.setStatus("COMPLETED");
                                    scanProvider.setCompletedAt(LocalDateTime.now());
                                }
                                scanProviderRepository.save(scanProvider);
                                break;

                            case "haveibeenpwned":
                            case "hibp":
                                if ("email".equals(request.getType())) {
                                    result = hibpService.checkEmailBreach(target).block();
                                    providerResults.put(provider + "_" + target, result);

                                    if (result != null && result.containsKey("error")) {
                                        String errorMsg = "✗ " + provider + " error: " + result.get("error");
                                        addLog(status, errorMsg);
                                        addLogToDB(scan, "ERROR", errorMsg);
                                        scanProvider.setStatus("FAILED");
                                        scanProvider.setErrorMessage(String.valueOf(result.get("error")));
                                    } else {
                                        String successMsg = "✓ " + provider + " query completed for " + target;
                                        addLog(status, successMsg);
                                        addLogToDB(scan, "INFO", successMsg);

                                        // Save result to DB
                                        saveScanResult(scan, scanTarget, scanProvider, result);

                                        scanProvider.setStatus("COMPLETED");
                                        scanProvider.setCompletedAt(LocalDateTime.now());
                                    }
                                    scanProviderRepository.save(scanProvider);
                                } else {
                                    addLog(status, "⚠ HaveIBeenPwned only supports email scan type");
                                    continue;
                                }
                                break;

                            default:
                                addLog(status, "⚠ Unknown provider: " + provider + " (skipped)");
                                continue;
                        }
                    } catch (Exception e) {
                        logger.error("Error querying " + provider + " for " + target, e);
                        String errorMsg = "✗ Error querying " + provider + " for " + target + ": " + e.getMessage();
                        addLog(status, errorMsg);
                        addLogToDB(scan, "ERROR", errorMsg);

                        // Store error in results
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", e.getMessage());
                        errorResult.put("exception", e.getClass().getSimpleName());
                        providerResults.put(provider + "_" + target + "_error", errorResult);

                        scanProvider.setStatus("FAILED");
                        scanProvider.setErrorMessage(e.getMessage());
                        scanProviderRepository.save(scanProvider);
                    }

                    // Simulate delay between requests
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Mark target as completed
                scanTarget.setStatus("COMPLETED");
                scanTarget.setProcessedAt(LocalDateTime.now());
                scanTargetRepository.save(scanTarget);
            }

            results.put("data", providerResults);

            // Update scan status
            scan.setStatus("COMPLETED");
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);

            status.setStatus("COMPLETED");
            status.setResult(results);

            String successMsg = "Scan completed successfully";
            addLog(status, successMsg);
            addLogToDB(scan, "INFO", successMsg);

        } catch (Exception e) {
            logger.error("Error executing scan", e);
            String errorMsg = "✗ Scan failed: " + e.getMessage();
            addLog(status, errorMsg);
            addLogToDB(scan, "ERROR", errorMsg);

            scan.setStatus("FAILED");
            scan.setErrorMessage(e.getMessage());
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);

            status.setStatus("FAILED");
            status.setResult(Map.of("error", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    public ScanStatus getScanStatus(String scanId) {
        // Try cache first
        ScanStatus cached = scanStatuses.get(scanId);
        if (cached != null) {
            return cached;
        }

        // Load from DB
        Optional<Scan> scanOpt = scanRepository.findByScanId(scanId);
        if (scanOpt.isEmpty()) {
            return null;
        }

        Scan scan = scanOpt.get();
        List<ScanLog> dbLogs = scanLogRepository.findByScanIdOrderByTimestampAsc(scan.getId());

        // Convert DB logs to LogEntry
        List<LogEntry> logs = dbLogs.stream()
                .map(log -> new LogEntry(
                        log.getTimestamp().toEpochSecond(ZoneOffset.UTC) * 1000,
                        log.getMessage()))
                .collect(Collectors.toList());

        // Build result map from scan results
        Map<String, Object> result = new HashMap<>();
        List<osint.model.ScanResult> scanResults = scanResultRepository.findByScanId(scan.getId());
        Map<String, Object> data = new HashMap<>();
        for (osint.model.ScanResult sr : scanResults) {
            if (sr.getScanTarget() != null) {
                data.put(sr.getProviderName() + "_" + sr.getScanTarget().getTarget(), sr.getResultData());
            }
        }
        result.put("data", data);

        ScanStatus status = new ScanStatus(scanId, scan.getStatus(), logs, result);
        scanStatuses.put(scanId, status); // Cache it
        return status;
    }

    private void addLog(ScanStatus status, String message) {
        status.getLogs().add(new LogEntry(System.currentTimeMillis(), message));
        logger.info("[Scan {}] {}", status.getScanId(), message);
    }

    private void addLogToDB(Scan scan, String level, String message) {
        ScanLog log = new ScanLog(scan, level, message);
        scanLogRepository.save(log);
        logger.info("[Scan {}] {}", scan.getScanId(), message);
    }

    private Entity updateOrCreateEntity(String entityType, String entityValue, Scan scan) {
        Optional<Entity> entityOpt = entityRepository.findByEntityTypeAndEntityValue(entityType, entityValue);
        Entity entity;

        if (entityOpt.isPresent()) {
            entity = entityOpt.get();
        } else {
            entity = new Entity(entityType, entityValue);
        }

        entity.setLastScan(scan);
        entity.setLastScannedAt(LocalDateTime.now());
        entity.setLastSeenAt(LocalDateTime.now());

        return entityRepository.save(entity);
    }

    private void saveScanResult(Scan scan, ScanTarget scanTarget, ScanProvider scanProvider,
            Map<String, Object> result) {
        osint.model.ScanResult scanResult = new osint.model.ScanResult();
        scanResult.setScan(scan);
        scanResult.setScanTarget(scanTarget);
        scanResult.setProviderName(scanProvider.getProviderName());
        scanResult.setResultData(result);

        // Calculate risk score if available
        if (result.containsKey("risk_score")) {
            Object riskObj = result.get("risk_score");
            if (riskObj instanceof Number) {
                scanResult.setRiskScore(BigDecimal.valueOf(((Number) riskObj).doubleValue()));
            }
        }

        if (result.containsKey("risk_level")) {
            scanResult.setRiskLevel(String.valueOf(result.get("risk_level")));
        }

        scanResultRepository.save(scanResult);
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
