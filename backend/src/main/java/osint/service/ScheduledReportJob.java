package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScheduledReportJob {
    private final ReportService reportService;
    private final boolean enabled;

    public ScheduledReportJob(ReportService reportService,
                              @Value("${REPORT_CRON_ENABLED:false}") boolean enabled) {
        this.reportService = reportService;
        this.enabled = enabled;
    }

    // Every day at 09:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyReport() {
        if (!enabled) return;
        reportService.createReport("Daily CyberScope Summary", List.of(
                Map.of("label", "High Risks", "value", "2"),
                Map.of("label", "Open Ports", "value", "3")
        ));
    }
}
