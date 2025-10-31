package osint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private final ReportService reportService;
    private final SmsService smsService;
    private final String alertEmail;
    private final String alertPhone;
    private final int emailThreshold;
    private final int smsThreshold;

    public AlertService(
            ReportService reportService,
            SmsService smsService,
            @Value("${ALERT_EMAIL:}") String alertEmail,
            @Value("${ALERT_PHONE:}") String alertPhone,
            @Value("${ALERT_EMAIL_THRESHOLD:60}") int emailThreshold,
            @Value("${ALERT_SMS_THRESHOLD:80}") int smsThreshold) {
        this.reportService = reportService;
        this.smsService = smsService;
        this.alertEmail = alertEmail;
        this.alertPhone = alertPhone;
        this.emailThreshold = emailThreshold;
        this.smsThreshold = smsThreshold;
    }

    public void handleRiskScore(String subject, String message, int riskScore) {
        if (riskScore >= emailThreshold && alertEmail != null && !alertEmail.isBlank()) {
            reportService.sendEmail(alertEmail, subject, message);
        }
        if (riskScore >= smsThreshold && alertPhone != null && !alertPhone.isBlank()) {
            smsService.sendSms(alertPhone, subject + ": " + message);
        }
    }
}
