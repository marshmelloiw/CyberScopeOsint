package osint.controller;

import osint.service.ShodanService;
import osint.service.VirusTotalService;
import osint.service.HaveIBeenPwnedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/osint")
public class OsintController {

    @Autowired
    private ShodanService shodanService;

    @Autowired
    private VirusTotalService virusTotalService;

    @Autowired
    private HaveIBeenPwnedService hibpService;

    // Shodan endpoints
    @GetMapping("/shodan/host/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> getShodanHostInfo(@PathVariable String ip) {
        return shodanService.getHostInfo(ip)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/shodan/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchShodan(@RequestParam String query) {
        return shodanService.searchHosts(query)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/shodan/domain/{domain}")
    public Mono<ResponseEntity<Map<String, Object>>> getShodanDomainInfo(@PathVariable String domain) {
        return shodanService.getDomainInfo(domain)
                .map(ResponseEntity::ok);
    }

    // VirusTotal endpoints
    @GetMapping("/virustotal/ip/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> getVirusTotalIpReport(@PathVariable String ip) {
        return virusTotalService.getIpReport(ip)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/virustotal/domain/{domain}")
    public Mono<ResponseEntity<Map<String, Object>>> getVirusTotalDomainReport(@PathVariable String domain) {
        return virusTotalService.getDomainReport(domain)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/virustotal/url")
    public Mono<ResponseEntity<Map<String, Object>>> getVirusTotalUrlReport(@RequestParam String url) {
        return virusTotalService.getUrlReport(url)
                .map(ResponseEntity::ok);
    }

    // HaveIBeenPwned endpoints
    @GetMapping("/hibp/breach/{email}")
    public Mono<ResponseEntity<Map<String, Object>>> checkEmailBreach(@PathVariable String email) {
        return hibpService.checkEmailBreach(email)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/hibp/breach-details/{breachName}")
    public Mono<ResponseEntity<Map<String, Object>>> getBreachDetails(@PathVariable String breachName) {
        return hibpService.getBreachDetails(breachName)
                .map(ResponseEntity::ok);
    }

    // Combined analysis endpoint
    @GetMapping("/analyze/ip/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeIp(@PathVariable String ip) {
        return Mono.zip(
                shodanService.getHostInfo(ip),
                virusTotalService.getIpReport(ip)).map(tuple -> {
                    Map<String, Object> result = Map.of(
                            "ip", ip,
                            "shodan", tuple.getT1(),
                            "virustotal", tuple.getT2(),
                            "timestamp", System.currentTimeMillis());
                    return ResponseEntity.ok(result);
                });
    }

    @GetMapping("/analyze/domain/{domain}")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeDomain(@PathVariable String domain) {
        return Mono.zip(
                shodanService.getDomainInfo(domain),
                virusTotalService.getDomainReport(domain)).map(tuple -> {
                    Map<String, Object> result = Map.of(
                            "domain", domain,
                            "shodan", tuple.getT1(),
                            "virustotal", tuple.getT2(),
                            "timestamp", System.currentTimeMillis());
                    return ResponseEntity.ok(result);
                });
    }
}
