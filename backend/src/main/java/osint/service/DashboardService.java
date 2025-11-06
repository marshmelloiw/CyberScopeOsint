package osint.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import osint.dto.DashboardSummaryResponse;
import osint.dto.DashboardSummaryResponse.Alert;
import osint.model.Scan;
import osint.model.ScanResult;
import osint.model.ScanTarget;
import osint.repository.ScanRepository;
import osint.repository.ScanResultRepository;

@Service
public class DashboardService {

    private static final List<String> ACTIVE_SCAN_STATUSES = Arrays.asList("RUNNING", "PENDING", "QUEUED");

    private final ScanRepository scanRepository;
    private final ScanResultRepository scanResultRepository;
    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService haveIBeenPwnedService;
    private final ZapService zapService;
    private final GeminiService geminiService;

    public DashboardService(
            ScanRepository scanRepository,
            ScanResultRepository scanResultRepository,
            ShodanService shodanService,
            VirusTotalService virusTotalService,
            HaveIBeenPwnedService haveIBeenPwnedService,
            ZapService zapService,
            GeminiService geminiService) {
        this.scanRepository = scanRepository;
        this.scanResultRepository = scanResultRepository;
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
}

