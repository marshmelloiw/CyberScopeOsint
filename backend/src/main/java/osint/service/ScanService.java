package osint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);
    
    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    
    // In-memory scan status storage (production'da Redis kullanılmalı)
    private final Map<String, ScanStatus> scanStatuses = new ConcurrentHashMap<>();
    
    @Autowired
    public ScanService(ShodanService shodanService,
                      VirusTotalService virusTotalService,
                      HaveIBeenPwnedService hibpService) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
    }
    
    public String startScan(ScanRequest request) {
        String scanId = UUID.randomUUID().toString();
        ScanStatus status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
        scanStatuses.put(scanId, status);
        
        // Execute async
        executeScanAsync(scanId, request);
        
        return scanId;
    }
    
    @Async("taskExecutor")
    private void executeScanAsync(String scanId, ScanRequest request) {
        ScanStatus status = scanStatuses.get(scanId);
        if (status == null) {
            status = new ScanStatus(scanId, "RUNNING", new ArrayList<>(), null);
            scanStatuses.put(scanId, status);
        }
        
        try {
            addLog(status, "Initializing scan for: " + request.getType());
            addLog(status, "Targets: " + String.join(", ", request.getTargets()));
            
            Map<String, Object> results = new HashMap<>();
            results.put("scanId", scanId);
            results.put("type", request.getType());
            results.put("targets", request.getTargets());
            results.put("providers", request.getProviders());
            results.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> providerResults = new HashMap<>();
            
            // Process each target
            for (String target : request.getTargets()) {
                addLog(status, "Processing target: " + target);
                
                // Process each provider
                for (String provider : request.getProviders()) {
                    addLog(status, "Querying " + provider + " for " + target + "...");
                    
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
                                    addLog(status, "✗ " + provider + " error: " + result.get("error"));
                                } else {
                                    addLog(status, "✓ " + provider + " query completed for " + target);
                                }
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
                                    addLog(status, "✗ " + provider + " error: " + result.get("error"));
                                } else {
                                    addLog(status, "✓ " + provider + " query completed for " + target);
                                }
                                break;
                                
                            case "haveibeenpwned":
                            case "hibp":
                                if ("email".equals(request.getType())) {
                                    result = hibpService.checkEmailBreach(target).block();
                                    providerResults.put(provider + "_" + target, result);
                                    
                                    if (result != null && result.containsKey("error")) {
                                        addLog(status, "✗ " + provider + " error: " + result.get("error"));
                                    } else {
                                        addLog(status, "✓ " + provider + " query completed for " + target);
                                    }
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
                        addLog(status, "✗ Error querying " + provider + " for " + target + ": " + e.getMessage());
                        // Store error in results
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("error", e.getMessage());
                        errorResult.put("exception", e.getClass().getSimpleName());
                        providerResults.put(provider + "_" + target + "_error", errorResult);
                    }
                    
                    // Simulate delay between requests
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            results.put("data", providerResults);
            status.setStatus("COMPLETED");
            status.setResult(results);
            addLog(status, "Scan completed successfully");
            
        } catch (Exception e) {
            logger.error("Error executing scan", e);
            status.setStatus("FAILED");
            addLog(status, "✗ Scan failed: " + e.getMessage());
            status.setResult(Map.of("error", e.getMessage()));
        }
    }
    
    public ScanStatus getScanStatus(String scanId) {
        return scanStatuses.get(scanId);
    }
    
    private void addLog(ScanStatus status, String message) {
        status.getLogs().add(new LogEntry(System.currentTimeMillis(), message));
        logger.info("[Scan {}] {}", status.getScanId(), message);
    }
    
    // Inner classes
    public static class ScanRequest {
        private String type;
        private List<String> targets;
        private List<String> providers;
        private String name;
        
        public ScanRequest() {}
        
        public ScanRequest(String type, List<String> targets, List<String> providers, String name) {
            this.type = type;
            this.targets = targets;
            this.providers = providers;
            this.name = name;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<String> getTargets() { return targets; }
        public void setTargets(List<String> targets) { this.targets = targets; }
        
        public List<String> getProviders() { return providers; }
        public void setProviders(List<String> providers) { this.providers = providers; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class ScanResult {
        private String scanId;
        private Map<String, Object> data;
        
        public ScanResult(String scanId, Map<String, Object> data) {
            this.scanId = scanId;
            this.data = data;
        }
        
        public String getScanId() { return scanId; }
        public Map<String, Object> getData() { return data; }
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
        public String getScanId() { return scanId; }
        public void setScanId(String scanId) { this.scanId = scanId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public List<LogEntry> getLogs() { return logs; }
        public void setLogs(List<LogEntry> logs) { this.logs = logs; }
        
        public Map<String, Object> getResult() { return result; }
        public void setResult(Map<String, Object> result) { this.result = result; }
    }
    
    public static class LogEntry {
        private long timestamp;
        private String message;
        
        public LogEntry(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
    }
}

