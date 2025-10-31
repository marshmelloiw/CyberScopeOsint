package osint.controller;

import osint.service.ScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/scans")
public class ScanController {
    
    private final ScanService scanService;
    
    @Autowired
    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }
    
    @PostMapping("/start")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı (JWT filter eksik)
    public ResponseEntity<Map<String, Object>> startScan(@RequestBody ScanService.ScanRequest request) {
        // Start async scan and return scanId immediately
        String scanId = scanService.startScan(request);
        
        return ResponseEntity.ok(Map.of(
            "scanId", scanId,
            "status", "started",
            "message", "Scan started successfully"
        ));
    }
    
    @GetMapping("/status/{scanId}")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<ScanService.ScanStatus> getScanStatus(@PathVariable String scanId) {
        ScanService.ScanStatus status = scanService.getScanStatus(scanId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/cli-simulate")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<Map<String, Object>> simulateCliCommand(@RequestBody Map<String, Object> request) {
        String command = (String) request.get("command");
        String target = (String) request.get("target");
        String type = (String) request.getOrDefault("type", "domain");
        
        // Start async scan
        ScanService.ScanRequest scanRequest = new ScanService.ScanRequest();
        scanRequest.setType(type);
        scanRequest.setTargets(java.util.List.of(target));
        scanRequest.setProviders(java.util.List.of("Shodan", "VirusTotal"));
        scanRequest.setName("CLI: " + command);
        
        String scanId = scanService.startScan(scanRequest);
        
        return ResponseEntity.ok(Map.of(
            "scanId", scanId,
            "command", command,
            "status", "executed"
        ));
    }
}

