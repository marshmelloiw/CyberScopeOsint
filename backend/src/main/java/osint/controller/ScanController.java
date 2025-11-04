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
}

