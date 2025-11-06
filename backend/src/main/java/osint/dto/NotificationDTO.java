package osint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NotificationDTO {
    private Integer id;
    private Long userId;
    private Long scanId; // Database ID
    private String scanIdString; // UUID string for frontend navigation
    private BigDecimal riskScore;
    private String riskLevel;
    private String message;
    private Boolean isRead;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    public NotificationDTO() {
    }
    
    public NotificationDTO(Integer id, Long userId, Long scanId, String scanIdString, BigDecimal riskScore, 
                          String riskLevel, String message, Boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.scanId = scanId;
        this.scanIdString = scanIdString;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
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
    
    public String getScanIdString() {
        return scanIdString;
    }
    
    public void setScanIdString(String scanIdString) {
        this.scanIdString = scanIdString;
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

