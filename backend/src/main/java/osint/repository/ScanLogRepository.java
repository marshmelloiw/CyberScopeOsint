package osint.repository;

import osint.model.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {
    
    List<ScanLog> findByScanId(Long scanId);
    
    List<ScanLog> findByScanIdOrderByTimestampAsc(Long scanId);
    
    List<ScanLog> findByLogLevel(String logLevel);
    
    @Query("SELECT sl FROM ScanLog sl WHERE sl.scan.id = :scanId AND sl.timestamp >= :since ORDER BY sl.timestamp ASC")
    List<ScanLog> findByScanIdAndSince(@Param("scanId") Long scanId, @Param("since") LocalDateTime since);
}

