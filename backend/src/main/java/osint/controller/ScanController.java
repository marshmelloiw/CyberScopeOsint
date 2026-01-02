package osint.controller;

import osint.service.ScanService;
import osint.service.ReportPdfService;
import osint.service.ReportHtmlService;
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
    private final ReportHtmlService reportHtmlService;
    
    @Autowired
    public ScanController(ScanService scanService, ReportPdfService reportPdfService, ReportHtmlService reportHtmlService) {
        this.scanService = scanService;
        this.reportPdfService = reportPdfService;
        this.reportHtmlService = reportHtmlService;
    }
    
    @PostMapping("/start")
    // @PreAuthorize("isAuthenticated()")
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
                "error", "Scan could not be started: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
    
    @GetMapping("/status/{scanId}")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScanService.ScanStatus> getScanStatus(@PathVariable String scanId) {
        ScanService.ScanStatus status = scanService.getScanStatus(scanId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping
    // @PreAuthorize("isAuthenticated()")
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
                "error", "Scan list could not be retrieved: " + e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{scanId}")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteScan(@PathVariable String scanId) {
        try {
            scanService.deleteScan(scanId);
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Scan successfully deleted",
                "scanId", scanId
            ));
        } catch (Exception e) {
            System.err.println("Error deleting scan: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "Scan could not be deleted: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/reports")
    // @PreAuthorize("isAuthenticated()")
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
                "error", "Reports could not be retrieved: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{scanId}/report/pdf")
    // @PreAuthorize("isAuthenticated()")
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
                "error", "PDF report could not be created: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{scanId}/report/html")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> downloadReportHtml(@PathVariable String scanId) {
        try {
            String htmlContent = reportHtmlService.generateHtmlReport(scanId);
            byte[] htmlBytes = htmlContent.getBytes("UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("attachment", "report_" + scanId + ".html");
            headers.setContentLength(htmlBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(htmlBytes);
        } catch (Exception e) {
            System.err.println("Error generating HTML report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", "HTML report could not be created: " + e.getMessage()
            ));
        }
    }
}

