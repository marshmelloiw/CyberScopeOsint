package osint.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import osint.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Create report", description = "Creates a report job and returns task id")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "CyberScope Report");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> items = (List<Map<String, String>>) body.getOrDefault("items", List.of());
        String taskId = reportService.createReport(title, items);
        return ResponseEntity.ok(Map.of("task_id", taskId));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get report status", description = "Returns report status and PDF when completed")
    public ResponseEntity<?> status(@PathVariable String taskId) {
        Map<String, Object> res = reportService.getReport(taskId);
        return ResponseEntity.ok(res);
    }
}
