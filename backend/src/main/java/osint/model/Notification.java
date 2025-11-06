package osint.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "scan_id", nullable = false)
    private Long scanId;
    
    @Column(name = "risk_score", precision = 3, scale = 1)
    private BigDecimal riskScore;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    public Notification(Long userId, Long scanId, BigDecimal riskScore, String riskLevel, String message) {
        this();
        this.userId = userId;
        this.scanId = scanId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.message = message;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getScanId() {
        return scanId;
    }
    
    public void setScanId(Long scanId) {
        this.scanId = scanId;
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
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
