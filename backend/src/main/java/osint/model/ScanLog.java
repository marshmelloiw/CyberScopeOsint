package osint.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "scan_logs")
public class ScanLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;
    
    @Column(name = "log_level", length = 20)
    private String logLevel; // INFO, WARN, ERROR, DEBUG
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    // Constructors
    public ScanLog() {
        this.timestamp = LocalDateTime.now();
        this.logLevel = "INFO";
    }
    
    public ScanLog(Scan scan, String message) {
        this();
        this.scan = scan;
        this.message = message;
    }
    
    public ScanLog(Scan scan, String logLevel, String message) {
        this(scan, message);
        this.logLevel = logLevel;
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
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

