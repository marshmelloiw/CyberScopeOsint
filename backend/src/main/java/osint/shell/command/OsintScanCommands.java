package osint.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;
import osint.service.ScanService;
import osint.service.ShodanService;
import osint.service.VirusTotalService;
import osint.service.HaveIBeenPwnedService;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Command(group = "OSINT Commands")
public class OsintScanCommands {
    
    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    
    @Autowired
    public OsintScanCommands(ShodanService shodanService,
                             VirusTotalService virusTotalService,
                             HaveIBeenPwnedService hibpService) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
    }
    
    @Command(command = "shodan", description = "Query Shodan for IP or domain information")
    public String shodanQuery(
        @Option(required = true, description = "IP address or domain") String target,
        @Option(description = "Query type: ip, domain, or search", defaultValue = "ip") String type
    ) {
        Mono<Map<String, Object>> result;
        
        switch (type.toLowerCase()) {
            case "ip":
                result = shodanService.getHostInfo(target);
                break;
            case "domain":
                result = shodanService.getDomainInfo(target);
                break;
            case "search":
                result = shodanService.searchHosts(target);
                break;
            default:
                return "Invalid type. Use: ip, domain, or search";
        }
        
        return result
            .map(data -> "Results: " + data.toString())
            .onErrorReturn("Error: " + target)
            .block();
    }
    
    @Command(command = "virustotal", description = "Query VirusTotal for threat intelligence")
    public String virusTotalQuery(
        @Option(required = true, description = "IP, domain, or URL") String target,
        @Option(description = "Query type: ip or domain", defaultValue = "ip") String type
    ) {
        Mono<Map<String, Object>> result;
        
        switch (type.toLowerCase()) {
            case "ip":
                result = virusTotalService.getIpReport(target);
                break;
            case "domain":
                result = virusTotalService.getDomainReport(target);
                break;
            default:
                return "Invalid type. Use: ip or domain";
        }
        
        return result
            .map(data -> "VirusTotal Results: " + data.toString())
            .onErrorReturn("Error querying VirusTotal")
            .block();
    }
    
    @Command(command = "domain-scan", description = "Perform comprehensive domain scan")
    public String domainScan(
        @Option(required = true, description = "Domain to scan") String domain
    ) {
        StringBuilder output = new StringBuilder();
        output.append("Starting domain scan for: ").append(domain).append("\n");
        
        // Shodan
        output.append("\n[Shodan] Querying...\n");
        try {
            Map<String, Object> shodanResult = shodanService.getDomainInfo(domain).block();
            output.append("Shodan: ").append(shodanResult.toString()).append("\n");
        } catch (Exception e) {
            output.append("Shodan error: ").append(e.getMessage()).append("\n");
        }
        
        // VirusTotal
        output.append("\n[VirusTotal] Querying...\n");
        try {
            Map<String, Object> vtResult = virusTotalService.getDomainReport(domain).block();
            output.append("VirusTotal: ").append(vtResult.toString()).append("\n");
        } catch (Exception e) {
            output.append("VirusTotal error: ").append(e.getMessage()).append("\n");
        }
        
        output.append("\nScan completed.");
        return output.toString();
    }
}

