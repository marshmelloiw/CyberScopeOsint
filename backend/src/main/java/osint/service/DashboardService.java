package osint.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import osint.dto.ChartDataResponse;
import osint.dto.DashboardSummaryResponse;
import osint.dto.DashboardSummaryResponse.Alert;
import osint.model.EntityFinding;
import osint.model.Scan;
import osint.model.ScanResult;
import osint.model.ScanTarget;
import osint.model.User;
import osint.repository.EntityFindingRepository;
import osint.repository.ScanRepository;
import osint.repository.ScanResultRepository;
import osint.repository.UserRepository;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final List<String> ACTIVE_SCAN_STATUSES = Arrays.asList("RUNNING", "PENDING", "QUEUED");

    private final ScanRepository scanRepository;
    private final ScanResultRepository scanResultRepository;
    private final EntityFindingRepository entityFindingRepository;
    private final UserRepository userRepository;
    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService haveIBeenPwnedService;
    private final ZapService zapService;
    private final GeminiService geminiService;

    public DashboardService(
            ScanRepository scanRepository,
            ScanResultRepository scanResultRepository,
            EntityFindingRepository entityFindingRepository,
            UserRepository userRepository,
            ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService haveIBeenPwnedService,
            ZapService zapService,
            GeminiService geminiService) {
        this.scanRepository = scanRepository;
        this.scanResultRepository = scanResultRepository;
        this.entityFindingRepository = entityFindingRepository;
        this.userRepository = userRepository;
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.haveIBeenPwnedService = haveIBeenPwnedService;
        this.zapService = zapService;
        this.geminiService = geminiService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        response.setOpenAlerts(scanResultRepository.countByFindingsCountGreaterThan(0));

        Double avgRisk = scanResultRepository.findAverageRiskScore();
        response.setAverageRiskScore(avgRisk != null ? round(avgRisk, 1) : 0.0);

        response.setActiveScans(scanRepository.countByStatusIn(ACTIVE_SCAN_STATUSES));

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        response.setNewFindings24h(scanResultRepository.countByCreatedAtAfter(since));

        List<ScanResult> recentResults = scanResultRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        response.setRecentAlerts(toAlertList(recentResults));

        DashboardSummaryResponse.ProviderStatus providers = response.getProviders();
        providers.setShodan(shodanService.isConfigured());
        providers.setVirusTotal(virusTotalService.isConfigured());
        providers.setHibp(haveIBeenPwnedService.isConfigured());
        providers.setZap(zapService.isConfigured());
        providers.setGemini(geminiService.isConfigured());

        return response;
    }

    private List<Alert> toAlertList(List<ScanResult> results) {
        List<Alert> alerts = new ArrayList<>();
        for (ScanResult result : results) {
            Alert alert = new Alert();
            alert.setId(result.getId());
            Scan scan = result.getScan();
            if (scan != null) {
                alert.setScanId(scan.getScanId());
            }

            alert.setSeverity(determineSeverity(result));
            alert.setEntity(resolveEntity(result));
            alert.setProvider(result.getProviderName());
            alert.setDescription(buildDescription(result));
            alert.setFindings(result.getFindingsCount());
            alert.setRiskScore(result.getRiskScore());
            alert.setCreatedAt(result.getCreatedAt());

            alerts.add(alert);
        }
        return alerts;
    }

    private String resolveEntity(ScanResult result) {
        ScanTarget target = result.getScanTarget();
        if (target != null && target.getTarget() != null && !target.getTarget().isEmpty()) {
            return target.getTarget();
        }
        Scan scan = result.getScan();
        if (scan != null) {
            if (scan.getName() != null && !scan.getName().isEmpty()) {
                return scan.getName();
            }
            if (scan.getScanId() != null) {
                return scan.getScanId();
            }
        }
        return "Unknown Target";
    }

    private String determineSeverity(ScanResult result) {
        String riskLevel = result.getRiskLevel();
        if (riskLevel != null && !riskLevel.isBlank()) {
            return riskLevel.toLowerCase();
        }

        BigDecimal score = result.getRiskScore();
        if (score != null) {
            double value = score.doubleValue();
            if (value >= 7.0) {
                return "high";
            }
            if (value >= 4.0) {
                return "medium";
            }
            return "low";
        }

        Integer findings = result.getFindingsCount();
        if (findings != null) {
            if (findings > 10) {
                return "high";
            }
            if (findings > 0) {
                return "medium";
            }
        }
        return "low";
    }

    private String buildDescription(ScanResult result) {
        Integer findings = result.getFindingsCount();
        BigDecimal score = result.getRiskScore();
        if (findings != null && findings > 0 && score != null) {
            return String.format("%d finding(s), risk score %.1f", findings, score.doubleValue());
        }
        if (findings != null && findings > 0) {
            return findings + " finding(s) detected";
        }
        if (score != null) {
            return String.format("Risk score %.1f", score.doubleValue());
        }
        return "No additional details available";
    }

    private double round(double value, int scale) {
        if (scale < 0) {
            return value;
        }
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    // Chart data methods
    @Transactional(readOnly = true)
    public ChartDataResponse.VulnerabilityData getVulnerabilityData(String scanId) {
        Optional<Scan> scanOpt = scanRepository.findByScanId(scanId);
        if (!scanOpt.isPresent()) {
            return new ChartDataResponse.VulnerabilityData(0, 0, 0, 0);
        }

        Scan scan = scanOpt.get();
        long critical = 0;
        long high = 0;
        long medium = 0;
        long low = 0;

        // First try to get from EntityFinding (if they exist and are linked to scan)
        List<EntityFinding> findings = entityFindingRepository.findByScanId(scan.getId());
        if (!findings.isEmpty()) {
            for (EntityFinding finding : findings) {
                String severity = finding.getSeverity();
                if (severity != null) {
                    String severityUpper = severity.toUpperCase();
                    switch (severityUpper) {
                        case "CRITICAL":
                            critical++;
                            break;
                        case "HIGH":
                            high++;
                            break;
                        case "MEDIUM":
                            medium++;
                            break;
                        case "LOW":
                            low++;
                            break;
                        default:
                            low++;
                            break;
                    }
                } else {
                    low++;
                }
            }
        }
        
        // Always also check ScanResult findings_count - this is the main source
        List<ScanResult> results = scanResultRepository.findByScanId(scan.getId());
        for (ScanResult result : results) {
            // Try to extract vulnerabilities from result_data if findings_count is 0 or null
            Map<String, Object> resultData = result.getResultData();
            if (resultData != null && !resultData.isEmpty()) {
                // Extract vulnerabilities from result_data based on provider
                Map<String, Long> extractedVulns = extractVulnerabilitiesFromResultData(resultData, result.getProviderName());
                critical += extractedVulns.getOrDefault("CRITICAL", 0L);
                high += extractedVulns.getOrDefault("HIGH", 0L);
                medium += extractedVulns.getOrDefault("MEDIUM", 0L);
                low += extractedVulns.getOrDefault("LOW", 0L);
            }
            
            // Also use findings_count if available
            Integer findingsCount = result.getFindingsCount();
            if (findingsCount != null && findingsCount > 0) {
                String riskLevel = result.getRiskLevel();
                String severityCategory = null;
                
                if (riskLevel != null && !riskLevel.isBlank()) {
                    severityCategory = riskLevel.toUpperCase();
                } else {
                    // Determine from risk score
                    BigDecimal score = result.getRiskScore();
                    if (score != null) {
                        double value = score.doubleValue();
                        if (value >= 8.0) {
                            severityCategory = "CRITICAL";
                        } else if (value >= 7.0) {
                            severityCategory = "HIGH";
                        } else if (value >= 4.0) {
                            severityCategory = "MEDIUM";
                        } else {
                            severityCategory = "LOW";
                        }
                    } else {
                        severityCategory = "LOW";
                    }
                }
                
                // Add findings_count to the appropriate category
                switch (severityCategory) {
                    case "CRITICAL":
                        critical += findingsCount;
                        break;
                    case "HIGH":
                        high += findingsCount;
                        break;
                    case "MEDIUM":
                        medium += findingsCount;
                        break;
                    case "LOW":
                    default:
                        low += findingsCount;
                        break;
                }
            }
        }

        return new ChartDataResponse.VulnerabilityData(critical, high, medium, low);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> extractVulnerabilitiesFromResultData(Map<String, Object> resultData, String providerName) {
        Map<String, Long> vulns = new HashMap<>();
        vulns.put("CRITICAL", 0L);
        vulns.put("HIGH", 0L);
        vulns.put("MEDIUM", 0L);
        vulns.put("LOW", 0L);

        if (resultData == null || resultData.isEmpty()) {
            return vulns;
        }

        try {
            String providerUpper = providerName != null ? providerName.toUpperCase() : "";
            
            // ZAP provider - check for alerts
            if (providerUpper.contains("ZAP") || providerUpper.equals("ZAP")) {
                Object alertsObj = resultData.get("alerts");
                if (alertsObj instanceof List) {
                    List<Map<String, Object>> alerts = (List<Map<String, Object>>) alertsObj;
                    for (Map<String, Object> alert : alerts) {
                        String risk = getStringValue(alert, "risk", "riskString", "riskdesc");
                        if (risk != null) {
                            String riskUpper = risk.toUpperCase();
                            if (riskUpper.contains("CRITICAL") || riskUpper.contains("HIGH") && riskUpper.contains("CRITICAL")) {
                                vulns.put("CRITICAL", vulns.get("CRITICAL") + 1);
                            } else if (riskUpper.contains("HIGH")) {
                                vulns.put("HIGH", vulns.get("HIGH") + 1);
                            } else if (riskUpper.contains("MEDIUM") || riskUpper.contains("MED")) {
                                vulns.put("MEDIUM", vulns.get("MEDIUM") + 1);
                            } else {
                                vulns.put("LOW", vulns.get("LOW") + 1);
                            }
                        } else {
                            // Check riskcode
                            Object riskCodeObj = alert.get("riskcode");
                            if (riskCodeObj != null) {
                                int riskCode = getIntValue(riskCodeObj);
                                if (riskCode >= 3) {
                                    vulns.put("CRITICAL", vulns.get("CRITICAL") + 1);
                                } else if (riskCode == 2) {
                                    vulns.put("HIGH", vulns.get("HIGH") + 1);
                                } else if (riskCode == 1) {
                                    vulns.put("MEDIUM", vulns.get("MEDIUM") + 1);
                                } else {
                                    vulns.put("LOW", vulns.get("LOW") + 1);
                                }
                            } else {
                                vulns.put("LOW", vulns.get("LOW") + 1);
                            }
                        }
                    }
                }
            }
            // VirusTotal provider
            else if (providerUpper.contains("VIRUSTOTAL") || providerUpper.equals("VIRUSTOTAL")) {
                // Check for malicious detections
                Object statsObj = resultData.get("stats");
                if (statsObj instanceof Map) {
                    Map<String, Object> stats = (Map<String, Object>) statsObj;
                    Object maliciousObj = stats.get("malicious");
                    if (maliciousObj != null) {
                        int malicious = getIntValue(maliciousObj);
                        if (malicious > 0) {
                            // Distribute based on malicious count
                            if (malicious >= 10) {
                                vulns.put("CRITICAL", vulns.get("CRITICAL") + malicious);
                            } else if (malicious >= 5) {
                                vulns.put("HIGH", vulns.get("HIGH") + malicious);
                            } else if (malicious >= 2) {
                                vulns.put("MEDIUM", vulns.get("MEDIUM") + malicious);
                            } else {
                                vulns.put("LOW", vulns.get("LOW") + malicious);
                            }
                        }
                    }
                }
                // Check for detections
                Object dataObj = resultData.get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) dataObj;
                    Object attributesObj = data.get("attributes");
                    if (attributesObj instanceof Map) {
                        Map<String, Object> attributes = (Map<String, Object>) attributesObj;
                        Object lastAnalysisStatsObj = attributes.get("last_analysis_stats");
                        if (lastAnalysisStatsObj instanceof Map) {
                            Map<String, Object> stats = (Map<String, Object>) lastAnalysisStatsObj;
                            Object maliciousObj = stats.get("malicious");
                            if (maliciousObj != null) {
                                int malicious = getIntValue(maliciousObj);
                                if (malicious > 0) {
                                    if (malicious >= 10) {
                                        vulns.put("CRITICAL", vulns.get("CRITICAL") + malicious);
                                    } else if (malicious >= 5) {
                                        vulns.put("HIGH", vulns.get("HIGH") + malicious);
                                    } else if (malicious >= 2) {
                                        vulns.put("MEDIUM", vulns.get("MEDIUM") + malicious);
                                    } else {
                                        vulns.put("LOW", vulns.get("LOW") + malicious);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // HIBP provider - check for breaches
            else if (providerUpper.contains("HIBP") || providerUpper.contains("HAVEIBEENPWNED")) {
                Object breachesObj = resultData.get("breaches");
                if (breachesObj instanceof List) {
                    List<?> breaches = (List<?>) breachesObj;
                    int breachCount = breaches.size();
                    if (breachCount > 0) {
                        // Each breach is considered a finding
                        if (breachCount >= 10) {
                            vulns.put("CRITICAL", vulns.get("CRITICAL") + breachCount);
                        } else if (breachCount >= 5) {
                            vulns.put("HIGH", vulns.get("HIGH") + breachCount);
                        } else if (breachCount >= 2) {
                            vulns.put("MEDIUM", vulns.get("MEDIUM") + breachCount);
                        } else {
                            vulns.put("LOW", vulns.get("LOW") + breachCount);
                        }
                    }
                }
            }
            // Shodan provider
            else if (providerUpper.contains("SHODAN")) {
                // Check for vulnerabilities
                Object vulnsObj = resultData.get("vulns");
                if (vulnsObj instanceof Map) {
                    Map<String, Object> vulnsMap = (Map<String, Object>) vulnsObj;
                    int vulnCount = vulnsMap.size();
                    if (vulnCount > 0) {
                        // Distribute based on count
                        if (vulnCount >= 10) {
                            vulns.put("CRITICAL", vulns.get("CRITICAL") + vulnCount);
                        } else if (vulnCount >= 5) {
                            vulns.put("HIGH", vulns.get("HIGH") + vulnCount);
                        } else if (vulnCount >= 2) {
                            vulns.put("MEDIUM", vulns.get("MEDIUM") + vulnCount);
                        } else {
                            vulns.put("LOW", vulns.get("LOW") + vulnCount);
                        }
                    }
                }
            }
            // Generic fallback - count non-empty, non-error fields
            else {
                int fieldCount = 0;
                for (Map.Entry<String, Object> entry : resultData.entrySet()) {
                    String key = entry.getKey().toLowerCase();
                    if (!key.equals("error") && !key.equals("provider") && entry.getValue() != null) {
                        Object value = entry.getValue();
                        if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                            fieldCount++;
                        } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                            fieldCount += ((List<?>) value).size();
                        } else if (!(value instanceof String && ((String) value).equalsIgnoreCase("error"))) {
                            fieldCount++;
                        }
                    }
                }
                if (fieldCount > 0) {
                    // Distribute based on field count
                    if (fieldCount >= 10) {
                        vulns.put("CRITICAL", vulns.get("CRITICAL") + fieldCount);
                    } else if (fieldCount >= 5) {
                        vulns.put("HIGH", vulns.get("HIGH") + fieldCount);
                    } else if (fieldCount >= 2) {
                        vulns.put("MEDIUM", vulns.get("MEDIUM") + fieldCount);
                    } else {
                        vulns.put("LOW", vulns.get("LOW") + fieldCount);
                    }
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty map
            logger.warn("Error extracting vulnerabilities from result_data for provider {}: {}", providerName, e.getMessage());
        }

        return vulns;
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private int getIntValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Transactional(readOnly = true)
    public ChartDataResponse.ToolUsageData getToolUsageData() {
        List<ScanResult> allResults = scanResultRepository.findAll();
        
        Map<String, Long> toolCounts = new HashMap<>();
        for (ScanResult result : allResults) {
            String provider = result.getProviderName();
            if (provider != null) {
                toolCounts.put(provider, toolCounts.getOrDefault(provider, 0L) + 1);
            }
        }

        List<Map<String, Object>> tools = toolCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> toolData = new HashMap<>();
                    toolData.put("name", entry.getKey());
                    toolData.put("count", entry.getValue());
                    return toolData;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());

        return new ChartDataResponse.ToolUsageData(tools);
    }

    @Transactional(readOnly = true)
    public ChartDataResponse.UserActivityData getUserActivityData() {
        List<User> allUsers = userRepository.findAll();
        
        long active = 0;
        long inactive = 0;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        if (allUsers.isEmpty()) {
            // No users, return zeros
            return new ChartDataResponse.UserActivityData(0, 0);
        }

        for (User user : allUsers) {
            LocalDateTime lastLogin = user.getLastLogin();
            // User is active if they logged in within last 30 days
            // If lastLogin is null, consider them inactive
            if (lastLogin != null && lastLogin.isAfter(thirtyDaysAgo)) {
                active++;
            } else {
                inactive++;
            }
        }

        return new ChartDataResponse.UserActivityData(active, inactive);
    }

    @Transactional(readOnly = true)
    public ChartDataResponse.ScanStatusData getScanStatusData() {
        List<Scan> allScans = scanRepository.findAll();
        
        Map<String, Long> statusCounts = new HashMap<>();
        for (Scan scan : allScans) {
            String status = scan.getStatus();
            if (status != null) {
                statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            }
        }

        List<Map<String, Object>> statuses = statusCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> statusData = new HashMap<>();
                    statusData.put("status", entry.getKey());
                    statusData.put("count", entry.getValue());
                    return statusData;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());

        return new ChartDataResponse.ScanStatusData(statuses);
    }
}

