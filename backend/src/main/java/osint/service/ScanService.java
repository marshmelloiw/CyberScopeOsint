package osint.service;

import osint.model.*;
import osint.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import osint.service.TwitterService.TwitterServiceException;

@Service
public class ScanService {

    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);
    private static final int TWITTER_DEFAULT_MAX_RESULTS = 50;

    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    private final GeminiService geminiService;
    private final ZapService zapService;
    private final TwitterService twitterService;

    private final EntityRepository entityRepository;
    private final ScanRepository scanRepository;
    private final ScanTargetRepository scanTargetRepository;
    private final ScanProviderRepository scanProviderRepository;
    private final ScanResultRepository scanResultRepository;
    private final ScanLogRepository scanLogRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;

    // In-memory cache for quick status lookups (still used for real-time status)
    private final Map<String, ScanStatus> scanStatuses = new ConcurrentHashMap<>();

    @Autowired
    public ScanService(
            ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService hibpService,
            GeminiService geminiService,
            ZapService zapService,
            TwitterService twitterService,
            EntityRepository entityRepository,
            ScanRepository scanRepository,
            ScanTargetRepository scanTargetRepository,
            ScanProviderRepository scanProviderRepository,
            ScanResultRepository scanResultRepository,
            ScanLogRepository scanLogRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            ApplicationContext applicationContext) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
        this.geminiService = geminiService;
        this.zapService = zapService;
        this.twitterService = twitterService;
        this.entityRepository = entityRepository;
        this.scanRepository = scanRepository;
        this.scanTargetRepository = scanTargetRepository;
        this.scanProviderRepository = scanProviderRepository;
        this.scanResultRepository = scanResultRepository;
        this.scanLogRepository = scanLogRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.applicationContext = applicationContext;
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
            // In production, get from SecurityContext:
            // SecurityContextHolder.getContext().getAuthentication()

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
            getSelf().executeScanAsyncWithDB(scan.getId(), scanId, request);

            return scanId;
        } catch (Exception e) {
            logger.error("Error starting scan (DB may not have scan tables yet): {}", e.getMessage(), e);
            // Fallback: still create in-memory status for API compatibility
            ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
            scanStatuses.put(scanId, status);
            // Try to execute without DB persistence as fallback
            getSelf().executeScanAsyncWithoutDB(scanId, request);
            return scanId;
        }
    }

    // Fallback method for when DB tables don't exist yet
    @Async("taskExecutor")
    public void executeScanAsyncWithoutDB(String scanId, ScanRequest request) {
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
                                    providerResult = zapService.performFullScan(target).block();
                                }
                                break;
                            case "TWITTER":
                                providerResult = handleTwitterProvider(target);
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
    public void executeScanAsyncWithDB(Long scanDbId, String scanId, ScanRequest request) {
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
                                    providerResult = zapService.performFullScan(target).block();
                                }
                                break;
                            case "TWITTER":
                                logger.info("Matched TWITTER for type: {}", request.getType());
                                providerResult = handleTwitterProvider(target);
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

                            // Calculate risk score based on findings if not already set
                            if (scanResult.getRiskScore() == null) {
                                scanResult.setRiskScore(calculateRiskScoreFromFindings(findingsCount, providerResult));

                                // Set risk level based on findings
                                if (findingsCount > 0) {
                                    if (findingsCount >= 8) {
                                        scanResult.setRiskLevel("CRITICAL");
                                    } else if (findingsCount >= 5) {
                                        scanResult.setRiskLevel("HIGH");
                                    } else if (findingsCount >= 3) {
                                        scanResult.setRiskLevel("MEDIUM");
                                    } else {
                                        scanResult.setRiskLevel("LOW");
                                    }
                                }
                            }

                            // Save to DB
                            scanResult = scanResultRepository.save(scanResult);
                            scanResultIds.put(provider + "_" + target, scanResult.getId());

                            // Check if risk_score > 7.5 or critical findings and create notification
                            checkAndCreateNotification(scanResult, scan);

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

    private ScanService getSelf() {
        return applicationContext.getBean(ScanService.class);
    }

    private Map<String, Object> handleTwitterProvider(String target) {
        if (!StringUtils.hasText(target)) {
            return Map.of("error", "Twitter target cannot be empty");
        }

        String normalized = target.trim();

        try {
            String query = normalized;
            String location = null;

            if (normalized.contains("|")) {
                String[] parts = normalized.split("\\|", 2);
                query = parts[0].trim();
                location = parts.length > 1 ? parts[1].trim() : null;
            }

            if (!StringUtils.hasText(query)) {
                return Map.of("error", "Twitter search query cannot be empty");
            }

            Map<String, Object> response = twitterService.searchRecentTweets(
                    query,
                    null,
                    StringUtils.hasText(location) ? location : null,
                    TWITTER_DEFAULT_MAX_RESULTS
            ).block();

            if (response == null) {
                return Map.of("error", "No response from Twitter API");
            }

            Map<String, Object> mutableResponse = new HashMap<>(response);
            mutableResponse.putIfAbsent("target", normalized);
            mutableResponse.putIfAbsent("provider", "Twitter");
            return mutableResponse;
        } catch (TwitterServiceException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", ex.getErrorCode());
            error.put("status", ex.getStatusCode());
            if (ex.getBody() != null) {
                error.put("details", ex.getBody());
            }
            return error;
        } catch (Exception ex) {
            logger.error("Twitter provider error for target {}: {}", target, ex.getMessage(), ex);
            return Map.of("error", ex.getMessage());
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
                                        Optional<ScanResult> scanResultOpt = scanResultRepository
                                                .findById(scanResultId);
                                        if (scanResultOpt.isPresent()) {
                                            ScanResult scanResult = scanResultOpt.get();
                                            scanResult.setGeminiReport(geminiAnalysis);

                                            // Extract risk_score and risk_level from Gemini report if available
                                            extractRiskFromGeminiReport(geminiAnalysis, scanResult);

                                            scanResult = scanResultRepository.save(scanResult);

                                            // Check if risk_score > 7.5 and create notification
                                            Optional<Scan> scanOpt = scanRepository
                                                    .findById(scanResult.getScan().getId());
                                            if (scanOpt.isPresent()) {
                                                checkAndCreateNotification(scanResult, scanOpt.get());
                                            }
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
        // First try in-memory cache
        ScanStatus status = scanStatuses.get(scanId);
        if (status != null) {
            return status;
        }

        // If not in cache, try to load from database
        try {
            Optional<Scan> scanOpt = scanRepository.findByScanId(scanId);
            if (!scanOpt.isPresent()) {
                logger.warn("Scan {} not found in database", scanId);
                return null;
            }

            Scan scan = scanOpt.get();

            // Build results map first (required for constructor)
            Map<String, Object> results = new HashMap<>();
            results.put("scanId", scan.getScanId());
            results.put("type", scan.getType());
            results.put("name", scan.getName());
            results.put("timestamp",
                    scan.getCreatedAt() != null
                            ? scan.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : System.currentTimeMillis());

            // Get targets
            List<ScanTarget> targets = scanTargetRepository.findByScanId(scan.getId());
            List<String> targetList = new ArrayList<>();
            for (ScanTarget target : targets) {
                targetList.add(target.getTarget());
            }
            results.put("targets", targetList);

            // Get providers
            List<ScanProvider> providers = scanProviderRepository.findByScanId(scan.getId());
            List<String> providerList = new ArrayList<>();
            for (ScanProvider provider : providers) {
                providerList.add(provider.getProviderName());
            }
            results.put("providers", providerList);

            // Get results and build provider results map
            Map<String, Object> providerResults = new HashMap<>();
            Map<String, Object> geminiReports = new HashMap<>();

            List<ScanResult> scanResults = scanResultRepository.findByScanId(scan.getId());
            for (ScanResult scanResult : scanResults) {
                String provider = scanResult.getProviderName();
                String target = scanResult.getScanTarget() != null ? scanResult.getScanTarget().getTarget() : "unknown";

                // Get raw result data
                Map<String, Object> providerResult = scanResult.getResultData();
                if (providerResult == null) {
                    providerResult = new HashMap<>();
                }

                // Add provider to result
                providerResult.put("provider", provider);

                // Build target result map
                if (!providerResults.containsKey(target)) {
                    providerResults.put(target, new HashMap<>());
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> targetResult = (Map<String, Object>) providerResults.get(target);
                targetResult.put(provider, providerResult);

                // Get Gemini report if exists
                if (scanResult.getGeminiReport() != null && !scanResult.getGeminiReport().isEmpty()) {
                    String key = provider + "_" + target;
                    geminiReports.put(key, scanResult.getGeminiReport());
                }
            }

            results.put("results", providerResults);
            results.put("data", providerResults);
            results.put("gemini_reports", geminiReports);

            // Build ScanStatus from database
            ScanStatus dbStatus = new ScanStatus(
                    scan.getScanId(),
                    scan.getStatus(),
                    new ArrayList<>(),
                    results);

            if (scan.getErrorMessage() != null) {
                dbStatus.setErrorMessage(scan.getErrorMessage());
            }

            if (scan.getCompletedAt() != null) {
                dbStatus.setCompletedAt(scan.getCompletedAt());
            }

            dbStatus.setResults(results);

            // Cache it for future requests
            scanStatuses.put(scanId, dbStatus);

            logger.debug("Loaded scan {} from database", scanId);
            return dbStatus;

        } catch (Exception e) {
            logger.error("Error loading scan {} from database: {}", scanId, e.getMessage(), e);
            return null;
        }
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
            throw new RuntimeException("Scan could not be deleted: " + e.getMessage(), e);
        }
    }

    /**
     * Get scans that have Gemini reports
     * Returns a list of scan summaries with Gemini reports
     */
    public List<Map<String, Object>> getScansWithGeminiReports() {
        List<Map<String, Object>> scanList = new ArrayList<>();

        try {
            // Get all completed scans from DB
            List<Scan> scans = scanRepository.findByStatus("COMPLETED");

            logger.debug("Found {} completed scans", scans.size());

            for (Scan scan : scans) {
                // Get scan results with Gemini reports
                List<ScanResult> results = scanResultRepository.findByScanId(scan.getId());
                boolean hasGeminiReport = false;
                Map<String, Object> geminiReportsMap = new HashMap<>();

                for (ScanResult result : results) {
                    if (result.getGeminiReport() != null && !result.getGeminiReport().isEmpty()) {
                        hasGeminiReport = true;
                        String key = result.getProviderName() + "_" +
                                (result.getScanTarget() != null ? result.getScanTarget().getTarget() : "unknown");
                        geminiReportsMap.put(key, result.getGeminiReport());
                    }
                }

                // Only include scans with Gemini reports
                if (hasGeminiReport) {
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

                    // Calculate findings count
                    int findingsCount = 0;
                    for (ScanResult result : results) {
                        if (result.getFindingsCount() != null) {
                            findingsCount += result.getFindingsCount();
                        }
                    }
                    scanSummary.put("findings", findingsCount);

                    // Add Gemini reports
                    scanSummary.put("geminiReports", geminiReportsMap);

                    // Convert timestamp
                    if (scan.getCreatedAt() != null) {
                        scanSummary.put("timestamp", scan.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli());
                    }

                    scanList.add(scanSummary);
                }
            }

        } catch (Exception e) {
            logger.error("Error getting scans with Gemini reports: {}", e.getMessage(), e);
            return scanList;
        }

        return scanList;
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
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                            "createdAt"));

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

    /**
     * Check if risk_score > 7.5 or critical findings and create notification if
     * needed
     */
    private void checkAndCreateNotification(ScanResult scanResult, Scan scan) {
        try {
            BigDecimal riskScore = scanResult.getRiskScore();
            Integer findingsCount = scanResult.getFindingsCount();
            String riskLevel = scanResult.getRiskLevel();

            // Determine if notification should be created
            boolean shouldCreateNotification = false;

            // Check 1: Risk score > 7.5
            if (riskScore != null && riskScore.compareTo(new BigDecimal("7.5")) > 0) {
                shouldCreateNotification = true;
            }
            // Check 2: Critical findings (8 or more findings)
            else if (findingsCount != null && findingsCount >= 8) {
                shouldCreateNotification = true;
                // Set risk score if not set
                if (riskScore == null) {
                    riskScore = new BigDecimal("8.0"); // Default high risk for critical findings
                    scanResult.setRiskScore(riskScore);
                }
                if (riskLevel == null || riskLevel.isBlank()) {
                    riskLevel = "CRITICAL";
                    scanResult.setRiskLevel(riskLevel);
                }
            }
            // Check 3: Risk level is CRITICAL
            else if (riskLevel != null && riskLevel.toUpperCase().equals("CRITICAL")) {
                shouldCreateNotification = true;
                // Set risk score if not set
                if (riskScore == null) {
                    riskScore = new BigDecimal("9.0"); // Default critical risk
                    scanResult.setRiskScore(riskScore);
                }
            }
            // Check 4: High findings count (5 or more)
            else if (findingsCount != null && findingsCount >= 5) {
                shouldCreateNotification = true;
                // Set risk score if not set
                if (riskScore == null) {
                    riskScore = new BigDecimal("7.5"); // Default high risk
                    scanResult.setRiskScore(riskScore);
                }
                if (riskLevel == null || riskLevel.isBlank()) {
                    riskLevel = "HIGH";
                    scanResult.setRiskLevel(riskLevel);
                }
            }

            if (shouldCreateNotification) {
                Long userId = scan.getUser() != null ? scan.getUser().getId() : null;
                if (userId == null) {
                    // Try to get first active user as fallback (for testing purposes)
                    logger.warn(
                            "Cannot create notification: user_id is null for scan {}. Attempting to find user from scan context.",
                            scan.getScanId());

                    try {
                        // First try to find user with ID 1 (most common case)
                        Optional<User> user1 = userRepository.findById(1L);
                        if (user1.isPresent() && Boolean.TRUE.equals(user1.get().getIsVerified())) {
                            userId = 1L;
                            logger.info("Using user ID 1 for notification (fallback)");
                        } else {
                            // If user 1 doesn't exist or not verified, get first verified user
                            Optional<User> firstUser = userRepository.findAll().stream()
                                    .filter(u -> Boolean.TRUE.equals(u.getIsVerified()))
                                    .findFirst();
                            if (firstUser.isPresent()) {
                                userId = firstUser.get().getId();
                                logger.info("Using fallback user {} for notification", userId);
                            } else {
                                logger.error("No verified user found. Cannot create notification for scan {}",
                                        scan.getScanId());
                                return;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error finding fallback user for notification: {}", e.getMessage());
                        return;
                    }
                }

                // Ensure risk level is set
                if (riskLevel == null || riskLevel.isBlank()) {
                    if (riskScore != null) {
                        double score = riskScore.doubleValue();
                        if (score >= 9.0) {
                            riskLevel = "CRITICAL";
                        } else if (score >= 7.5) {
                            riskLevel = "HIGH";
                        } else {
                            riskLevel = "MEDIUM";
                        }
                    } else {
                        riskLevel = "HIGH";
                    }
                }

                String target = scanResult.getScanTarget() != null ? scanResult.getScanTarget().getTarget() : "unknown";
                String provider = scanResult.getProviderName();

                // Build message
                String message;
                if (findingsCount != null && findingsCount > 0) {
                    if (riskScore != null) {
                        message = String.format(
                                "High risk detected: %d findings in %s scan for %s, risk score %.1f (Level: %s)",
                                findingsCount, provider, target, riskScore.doubleValue(), riskLevel);
                    } else {
                        message = String.format(
                                "High risk detected: %d findings in %s scan for %s (Level: %s)",
                                findingsCount, provider, target, riskLevel);
                    }
                } else {
                    message = String.format(
                            "High risk score detected: Risk score %.1f in %s scan for %s (Level: %s)",
                            riskScore != null ? riskScore.doubleValue() : 0.0, provider, target, riskLevel);
                }

                notificationService.createNotification(
                        userId,
                        scan.getId(),
                        riskScore != null ? riskScore : new BigDecimal("7.5"),
                        riskLevel,
                        message);

                logger.info("Notification created for scan {} - findings: {}, risk_score: {}, risk_level: {}",
                        scan.getScanId(), findingsCount, riskScore, riskLevel);
            }
        } catch (Exception e) {
            logger.error("Error creating notification for scan result {}: {}", scanResult.getId(), e.getMessage(), e);
        }
    }

    /**
     * Calculate risk score from findings count and provider result
     */
    private BigDecimal calculateRiskScoreFromFindings(int findingsCount, Map<String, Object> providerResult) {
        if (findingsCount == 0) {
            return new BigDecimal("0.0");
        }

        // Base score from findings count (0-10 scale)
        double baseScore = Math.min(10.0, findingsCount * 1.2);

        // Check for error in result
        if (providerResult.containsKey("error")) {
            baseScore = Math.max(baseScore, 5.0); // At least medium risk if there's an error
        }

        // Adjust based on findings count thresholds
        if (findingsCount >= 8) {
            baseScore = Math.max(baseScore, 8.5); // Critical
        } else if (findingsCount >= 5) {
            baseScore = Math.max(baseScore, 7.5); // High
        } else if (findingsCount >= 3) {
            baseScore = Math.max(baseScore, 5.0); // Medium
        }

        return BigDecimal.valueOf(Math.min(10.0, baseScore));
    }

    /**
     * Extract risk_score and risk_level from Gemini report
     */
    @SuppressWarnings("unchecked")
    private void extractRiskFromGeminiReport(Map<String, Object> geminiAnalysis, ScanResult scanResult) {
        try {
            // Try to extract from structured analysis
            if (geminiAnalysis.containsKey("analysis")) {
                Object analysisObj = geminiAnalysis.get("analysis");
                if (analysisObj instanceof Map) {
                    Map<String, Object> analysis = (Map<String, Object>) analysisObj;

                    // Look for risk_score in various possible locations
                    if (analysis.containsKey("risk_score")) {
                        Object riskScoreObj = analysis.get("risk_score");
                        if (riskScoreObj instanceof Number) {
                            scanResult.setRiskScore(BigDecimal.valueOf(((Number) riskScoreObj).doubleValue()));
                        }
                    }

                    if (analysis.containsKey("risk_level") || analysis.containsKey("riskLevel")) {
                        Object riskLevelObj = analysis.getOrDefault("risk_level", analysis.get("riskLevel"));
                        if (riskLevelObj != null) {
                            scanResult.setRiskLevel(riskLevelObj.toString().toUpperCase());
                        }
                    }

                    // Check in Risk Assessment section
                    if (analysis.containsKey("Risk Assessment")) {
                        Object riskAssessmentObj = analysis.get("Risk Assessment");
                        if (riskAssessmentObj instanceof Map) {
                            Map<String, Object> riskAssessment = (Map<String, Object>) riskAssessmentObj;
                            if (riskAssessment.containsKey("score")) {
                                Object scoreObj = riskAssessment.get("score");
                                if (scoreObj instanceof Number) {
                                    scanResult.setRiskScore(BigDecimal.valueOf(((Number) scoreObj).doubleValue()));
                                }
                            }
                            if (riskAssessment.containsKey("level")) {
                                Object levelObj = riskAssessment.get("level");
                                if (levelObj != null) {
                                    scanResult.setRiskLevel(levelObj.toString().toUpperCase());
                                }
                            }
                        }
                    }
                }
            }

            // Try to extract from markdown text using regex
            if (scanResult.getRiskScore() == null && geminiAnalysis.containsKey("markdown")) {
                String markdown = geminiAnalysis.get("markdown").toString();

                // Look for risk score pattern: "score 7.5" or "score: 7.5" or "7.5/10"
                java.util.regex.Pattern scorePattern = java.util.regex.Pattern.compile(
                        "(?:risk[\\s_-]?score|score)[\\s:]*([0-9]+(?:\\.[0-9]+)?)",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher scoreMatcher = scorePattern.matcher(markdown);
                if (scoreMatcher.find()) {
                    try {
                        double score = Double.parseDouble(scoreMatcher.group(1));
                        scanResult.setRiskScore(BigDecimal.valueOf(score));
                    } catch (NumberFormatException e) {
                        logger.debug("Could not parse risk score from markdown");
                    }
                }

                // Look for risk level pattern: "HIGH", "MEDIUM", "LOW", "CRITICAL"
                if (scanResult.getRiskLevel() == null) {
                    java.util.regex.Pattern levelPattern = java.util.regex.Pattern.compile(
                            "(?:risk[\\s_-]?level|level)[\\s:]*\\b(CRITICAL|HIGH|MEDIUM|LOW)\\b",
                            java.util.regex.Pattern.CASE_INSENSITIVE);
                    java.util.regex.Matcher levelMatcher = levelPattern.matcher(markdown);
                    if (levelMatcher.find()) {
                        scanResult.setRiskLevel(levelMatcher.group(1).toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting risk from Gemini report: {}", e.getMessage());
        }
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
