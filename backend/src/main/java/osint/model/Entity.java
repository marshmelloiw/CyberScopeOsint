package osint.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "entities")
public class Entity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "entity_type", length = 50)
    private String entityType; // DOMAIN, IP, EMAIL, HANDLE
    
    @Column(name = "entity_value", nullable = false, length = 255)
    private String entityValue;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_scan_at")
    private LocalDateTime lastScanAt;
    
    // Transient fields - not in DB schema but used for backward compatibility
    @Transient
    private Scan lastScan;
    
    @Transient
    private BigDecimal riskScore;
    
    @Transient
    private String riskLevel;
    
    @Transient
    private LocalDateTime firstSeenAt;
    
    @Transient
    private LocalDateTime lastSeenAt;
    
    @Transient
    private List<EntityFinding> findings = new ArrayList<>();
    
    // Constructors
    public Entity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Entity(String entityType, String entityValue) {
        this();
        this.entityType = entityType;
        this.entityValue = entityValue;
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
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastScanAt() {
        return lastScanAt;
    }
    
    public void setLastScanAt(LocalDateTime lastScanAt) {
        this.lastScanAt = lastScanAt;
    }
    
    // Backward compatibility methods
    public Scan getLastScan() {
        return lastScan;
    }
    
    public void setLastScan(Scan lastScan) {
        this.lastScan = lastScan;
    }
    
    public LocalDateTime getLastScannedAt() {
        return lastScanAt;
    }
    
    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScanAt = lastScannedAt;
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
        return createdAt != null ? createdAt : firstSeenAt;
    }
    
    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }
    
    public LocalDateTime getLastSeenAt() {
        return lastScanAt != null ? lastScanAt : lastSeenAt;
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

