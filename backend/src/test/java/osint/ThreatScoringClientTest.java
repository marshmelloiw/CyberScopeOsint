package osint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import osint.service.ThreatScoringClient;

class ThreatScoringClientTest {
    @Test
    void canConstructClient() {
        ThreatScoringClient client = new ThreatScoringClient("http://localhost:18000");
        Map<String, Object> payload = Map.of("provider", Map.of());
        // Not calling remote; just ensure no NPE on creation
        assertThat(client).isNotNull();
    }
}
