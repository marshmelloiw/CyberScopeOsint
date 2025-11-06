package osint.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "enable_notifications", nullable = false)
    private Boolean enableNotifications = true;

    @Column(name = "sound_alerts", nullable = false)
    private Boolean soundAlerts = true;

    @Column(name = "category_security", nullable = false)
    private Boolean categorySecurity = true;

    @Column(name = "category_scan", nullable = false)
    private Boolean categoryScan = true;

    @Column(name = "category_breach", nullable = false)
    private Boolean categoryBreach = true;

    @Column(name = "category_system", nullable = false)
    private Boolean categorySystem = true;

    @Column(name = "category_intelligence", nullable = false)
    private Boolean categoryIntelligence = true;

    @Column(name = "in_app_notifications", nullable = false)
    private Boolean inAppNotifications = true;

    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = true;

    @Column(name = "push_notifications", nullable = false)
    private Boolean pushNotifications = false;

    @Column(name = "digest_frequency", length = 20, nullable = false)
    private String digestFrequency = "daily";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public NotificationPreferences() {
        this.enableNotifications = true;
        this.soundAlerts = true;
        this.categorySecurity = true;
        this.categoryScan = true;
        this.categoryBreach = true;
        this.categorySystem = true;
        this.categoryIntelligence = true;
        this.inAppNotifications = true;
        this.emailNotifications = true;
        this.pushNotifications = false;
        this.digestFrequency = "daily";
    }

    public NotificationPreferences(Long userId) {
        this();
        this.userId = userId;
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

    public Boolean getEnableNotifications() {
        return enableNotifications;
    }

    public void setEnableNotifications(Boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    public Boolean getSoundAlerts() {
        return soundAlerts;
    }

    public void setSoundAlerts(Boolean soundAlerts) {
        this.soundAlerts = soundAlerts;
    }

    public Boolean getCategorySecurity() {
        return categorySecurity;
    }

    public void setCategorySecurity(Boolean categorySecurity) {
        this.categorySecurity = categorySecurity;
    }

    public Boolean getCategoryScan() {
        return categoryScan;
    }

    public void setCategoryScan(Boolean categoryScan) {
        this.categoryScan = categoryScan;
    }

    public Boolean getCategoryBreach() {
        return categoryBreach;
    }

    public void setCategoryBreach(Boolean categoryBreach) {
        this.categoryBreach = categoryBreach;
    }

    public Boolean getCategorySystem() {
        return categorySystem;
    }

    public void setCategorySystem(Boolean categorySystem) {
        this.categorySystem = categorySystem;
    }

    public Boolean getCategoryIntelligence() {
        return categoryIntelligence;
    }

    public void setCategoryIntelligence(Boolean categoryIntelligence) {
        this.categoryIntelligence = categoryIntelligence;
    }

    public Boolean getInAppNotifications() {
        return inAppNotifications;
    }

    public void setInAppNotifications(Boolean inAppNotifications) {
        this.inAppNotifications = inAppNotifications;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(Boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public String getDigestFrequency() {
        return digestFrequency;
    }

    public void setDigestFrequency(String digestFrequency) {
        this.digestFrequency = digestFrequency;
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

