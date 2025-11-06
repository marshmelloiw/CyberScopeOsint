package osint.controller;

import osint.service.ScanService;
import osint.service.ReportPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scans")
public class ScanController {
    
    private final ScanService scanService;
    private final ReportPdfService reportPdfService;
    
    @Autowired
    public ScanController(ScanService scanService, ReportPdfService reportPdfService) {
        this.scanService = scanService;
        this.reportPdfService = reportPdfService;
    }
    
    @PostMapping("/start")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı (JWT filter eksik)
    public ResponseEntity<?> startScan(@RequestBody ScanService.ScanRequest request) {
        try {
            // Start async scan and return scanId immediately
            String scanId = scanService.startScan(request);
            
            return ResponseEntity.ok(Map.of(
                "scanId", scanId,
                "status", "started",
                "message", "Scan started successfully"
            ));
        } catch (Exception e) {
            // Log error for debugging
            System.err.println("Error starting scan: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(Map.of(
                "error", "Scan başlatılamadı: " + e.getMessage(),
                "status", "error"
            ));
        }
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
    
    @GetMapping
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<?> getAllScans() {
        try {
            java.util.List<java.util.Map<String, Object>> scans = scanService.getAllScans();
            return ResponseEntity.ok(java.util.Map.of(
                "scans", scans,
                "total", scans.size()
            ));
        } catch (Exception e) {
            System.err.println("Error getting scans: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Scan listesi alınamadı: " + e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{scanId}")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<?> deleteScan(@PathVariable String scanId) {
        try {
            scanService.deleteScan(scanId);
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Scan başarıyla silindi",
                "scanId", scanId
            ));
        } catch (Exception e) {
            System.err.println("Error deleting scan: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Scan silinemedi: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/reports")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<?> getGeminiReports() {
        try {
            java.util.List<java.util.Map<String, Object>> reports = scanService.getScansWithGeminiReports();
            return ResponseEntity.ok(java.util.Map.of(
                "reports", reports,
                "total", reports.size()
            ));
        } catch (Exception e) {
            System.err.println("Error getting Gemini reports: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Raporlar alınamadı: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{scanId}/report/pdf")
    // @PreAuthorize("isAuthenticated()")  // Geçici olarak devre dışı
    public ResponseEntity<?> downloadReportPdf(@PathVariable String scanId) {
        try {
            byte[] pdfBytes = reportPdfService.generatePdfReport(scanId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report_" + scanId + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
        } catch (Exception e) {
            System.err.println("Error generating PDF report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "PDF raporu oluşturulamadı: " + e.getMessage()
            ));
        }
    }
}

