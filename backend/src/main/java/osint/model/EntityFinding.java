package osint.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@jakarta.persistence.Entity
@Table(name = "entity_findings")
public class EntityFinding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private Entity entity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;
    
    @Column(name = "finding_type", nullable = false, length = 50)
    private String findingType; // CVE, BREACH, VULNERABILITY, MALWARE, SUSPICIOUS
    
    @Column(name = "finding_id", length = 255)
    private String findingId; // CVE-2021-1234, breach name, etc.
    
    @Column(name = "title", length = 500)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "cvss_score", precision = 3, scale = 1)
    private BigDecimal cvssScore;
    
    @Column(name = "source", length = 100)
    private String source; // Which provider found this
    
    @Column(name = "raw_data", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawData;
    
    @Column(name = "discovered_at", nullable = false, updatable = false)
    private LocalDateTime discoveredAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "status", length = 50)
    private String status = "ACTIVE"; // ACTIVE, RESOLVED, FALSE_POSITIVE
    
    // Constructors
    public EntityFinding() {
        this.discoveredAt = LocalDateTime.now();
    }
    
    public EntityFinding(Entity entity, String findingType, String findingId) {
        this();
        this.entity = entity;
        this.findingType = findingType;
        this.findingId = findingId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    
    public Scan getScan() {
        return scan;
    }
    
    public void setScan(Scan scan) {
        this.scan = scan;
    }
    
    public String getFindingType() {
        return findingType;
    }
    
    public void setFindingType(String findingType) {
        this.findingType = findingType;
    }
    
    public String getFindingId() {
        return findingId;
    }
    
    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public BigDecimal getCvssScore() {
        return cvssScore;
    }
    
    public void setCvssScore(BigDecimal cvssScore) {
        this.cvssScore = cvssScore;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Map<String, Object> getRawData() {
        return rawData;
    }
    
    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }
    
    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }
    
    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }
    
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

