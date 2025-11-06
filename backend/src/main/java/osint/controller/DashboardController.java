package osint.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import osint.dto.ChartDataResponse;
import osint.dto.DashboardSummaryResponse;
import osint.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/charts/vulnerabilities/{scanId}")
    public ResponseEntity<ChartDataResponse.VulnerabilityData> getVulnerabilityData(@PathVariable String scanId) {
        return ResponseEntity.ok(dashboardService.getVulnerabilityData(scanId));
    }

    @GetMapping("/charts/tools")
    public ResponseEntity<ChartDataResponse.ToolUsageData> getToolUsageData() {
        return ResponseEntity.ok(dashboardService.getToolUsageData());
    }

    @GetMapping("/charts/users")
    public ResponseEntity<ChartDataResponse.UserActivityData> getUserActivityData() {
        return ResponseEntity.ok(dashboardService.getUserActivityData());
    }

    @GetMapping("/charts/scans")
    public ResponseEntity<ChartDataResponse.ScanStatusData> getScanStatusData() {
        return ResponseEntity.ok(dashboardService.getScanStatusData());
    }
}

