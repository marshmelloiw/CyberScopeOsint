package osint.model;

import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "entities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"entity_type", "entity_value"})
})
public class Entity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // domain, ip, email, url, file_hash
    
    @Column(name = "entity_value", nullable = false, length = 500)
    private String entityValue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_scan_id")
    private Scan lastScan;
    
    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;
    
    @Column(name = "risk_score", precision = 3, scale = 1)
    private BigDecimal riskScore;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private LocalDateTime firstSeenAt;
    
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
    
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityFinding> findings = new ArrayList<>();
    
    // Constructors
    public Entity() {
        LocalDateTime now = LocalDateTime.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }
    
    public Entity(String entityType, String entityValue) {
        this();
        this.entityType = entityType;
        this.entityValue = entityValue;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastSeenAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityValue() {
        return entityValue;
    }
    
    public void setEntityValue(String entityValue) {
        this.entityValue = entityValue;
    }
    
    public Scan getLastScan() {
        return lastScan;
    }
    
    public void setLastScan(Scan lastScan) {
        this.lastScan = lastScan;
    }
    
    public LocalDateTime getLastScannedAt() {
        return lastScannedAt;
    }
    
    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScannedAt = lastScannedAt;
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
    
    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }
    
    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }
    
    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
    
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
    
    public List<EntityFinding> getFindings() {
        return findings;
    }
    
    public void setFindings(List<EntityFinding> findings) {
        this.findings = findings;
    }
}

