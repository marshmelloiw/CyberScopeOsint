package osint.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ReportService {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF = new ParameterizedTypeReference<>() {};

    private final WebClient client;
    private final JavaMailSender mailSender;

    public ReportService(
            @Value("${REPORT_SERVICE_URL:http://localhost:8010}") String baseUrl,
            JavaMailSender mailSender) {
        this.client = WebClient.builder().baseUrl(baseUrl).build();
        this.mailSender = mailSender;
    }

    public String createReport(String title, List<Map<String, String>> items) {
        Map<String, Object> payload = Map.of(
            "title", title,
            "items", items
        );
        Map<String, Object> res = client.post().uri("/reports")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(MAP_REF)
            .timeout(Duration.ofSeconds(5))
            .block();
        return res == null ? null : (String) res.get("task_id");
    }

    public Map<String, Object> getReport(String taskId) {
        return client.get().uri("/reports/" + taskId)
            .retrieve()
            .bodyToMono(MAP_REF)
            .timeout(Duration.ofSeconds(5))
            .block();
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
