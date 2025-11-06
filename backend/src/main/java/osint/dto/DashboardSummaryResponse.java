package osint.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummaryResponse {

    private long openAlerts;
    private double averageRiskScore;
    private long activeScans;
    private long newFindings24h;
    private List<Alert> recentAlerts = new ArrayList<>();
    private ProviderStatus providers = new ProviderStatus();

    public long getOpenAlerts() {
        return openAlerts;
    }

    public void setOpenAlerts(long openAlerts) {
        this.openAlerts = openAlerts;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(double averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public long getActiveScans() {
        return activeScans;
    }

    public void setActiveScans(long activeScans) {
        this.activeScans = activeScans;
    }

    public long getNewFindings24h() {
        return newFindings24h;
    }

    public void setNewFindings24h(long newFindings24h) {
        this.newFindings24h = newFindings24h;
    }

    public List<Alert> getRecentAlerts() {
        return recentAlerts;
    }

    public void setRecentAlerts(List<Alert> recentAlerts) {
        this.recentAlerts = recentAlerts;
    }

    public ProviderStatus getProviders() {
        return providers;
    }

    public void setProviders(ProviderStatus providers) {
        this.providers = providers;
    }

    public static class Alert {
        private Long id;
        private String scanId;
        private String severity;
        private String entity;
        private String provider;
        private String description;
        private Integer findings;
        private BigDecimal riskScore;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getScanId() {
            return scanId;
        }

        public void setScanId(String scanId) {
            this.scanId = scanId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getFindings() {
            return findings;
        }

        public void setFindings(Integer findings) {
            this.findings = findings;
        }

        public BigDecimal getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(BigDecimal riskScore) {
            this.riskScore = riskScore;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class ProviderStatus {
        private boolean apiOnline = true;
        private boolean databaseConnected = true;
        private boolean shodan;
        private boolean virusTotal;
        private boolean hibp;
        private boolean zap;
        private boolean gemini;

        public boolean isApiOnline() {
            return apiOnline;
        }

        public void setApiOnline(boolean apiOnline) {
            this.apiOnline = apiOnline;
        }

        public boolean isDatabaseConnected() {
            return databaseConnected;
        }

        public void setDatabaseConnected(boolean databaseConnected) {
            this.databaseConnected = databaseConnected;
        }

        public boolean isShodan() {
            return shodan;
        }

        public void setShodan(boolean shodan) {
            this.shodan = shodan;
        }

        public boolean isVirusTotal() {
            return virusTotal;
        }

        public void setVirusTotal(boolean virusTotal) {
            this.virusTotal = virusTotal;
        }

        public boolean isHibp() {
            return hibp;
        }

        public void setHibp(boolean hibp) {
            this.hibp = hibp;
        }

        public boolean isZap() {
            return zap;
        }

        public void setZap(boolean zap) {
            this.zap = zap;
        }

        public boolean isGemini() {
            return gemini;
        }

        public void setGemini(boolean gemini) {
            this.gemini = gemini;
        }
    }
}

