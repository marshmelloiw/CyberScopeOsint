package osint.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "scans")
public class Scan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "scan_id", unique = true, nullable = false)
    private String scanId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "type", nullable = false)
    private String type; // domain, ip, email, url, file
    
    @Column(name = "status", nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "priority")
    private String priority; // LOW, NORMAL, HIGH, CRITICAL
    
    @Column(name = "schedule_id")
    private Long scheduleId;
    
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanTarget> targets = new ArrayList<>();
    
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanProvider> providers = new ArrayList<>();
    
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanResult> results = new ArrayList<>();
    
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanLog> logs = new ArrayList<>();
    
    // Constructors
    public Scan() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Scan(String scanId, String type, String status) {
        this();
        this.scanId = scanId;
        this.type = type;
        this.status = status;
    }
    
    // Getters and Setters
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public Long getScheduleId() {
        return scheduleId;
    }
    
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
    
    public List<ScanTarget> getTargets() {
        return targets;
    }
    
    public void setTargets(List<ScanTarget> targets) {
        this.targets = targets;
    }
    
    public List<ScanProvider> getProviders() {
        return providers;
    }
    
    public void setProviders(List<ScanProvider> providers) {
        this.providers = providers;
    }
    
    public List<ScanResult> getResults() {
        return results;
    }
    
    public void setResults(List<ScanResult> results) {
        this.results = results;
    }
    
    public List<ScanLog> getLogs() {
        return logs;
    }
    
    public void setLogs(List<ScanLog> logs) {
        this.logs = logs;
    }
}

