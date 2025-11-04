package osint.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@jakarta.persistence.Entity
@Table(name = "scan_results")
public class ScanResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_target_id")
    private ScanTarget scanTarget;
    
    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;
    
    @Column(name = "result_data", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> resultData;
    
    @Column(name = "risk_score", precision = 3, scale = 1)
    private BigDecimal riskScore;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "findings_count")
    private Integer findingsCount = 0;
    
    @Column(name = "gemini_report", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> geminiReport;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public ScanResult() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ScanResult(Scan scan, String providerName, Map<String, Object> resultData) {
        this();
        this.scan = scan;
        this.providerName = providerName;
        this.resultData = resultData;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Scan getScan() {
        return scan;
    }
    
    public void setScan(Scan scan) {
        this.scan = scan;
    }
    
    public ScanTarget getScanTarget() {
        return scanTarget;
    }
    
    public void setScanTarget(ScanTarget scanTarget) {
        this.scanTarget = scanTarget;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public Map<String, Object> getResultData() {
        return resultData;
    }
    
    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }
    
    public BigDecimal getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public Integer getFindingsCount() {
        return findingsCount;
    }
    
    public void setFindingsCount(Integer findingsCount) {
        this.findingsCount = findingsCount;
    }
    
    public Map<String, Object> getGeminiReport() {
        return geminiReport;
    }
    
    public void setGeminiReport(Map<String, Object> geminiReport) {
        this.geminiReport = geminiReport;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

