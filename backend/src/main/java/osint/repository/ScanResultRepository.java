package osint.repository;

import osint.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

// @Repository - Disabled: Scan tables don't exist
public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {
    
    List<ScanResult> findByScanId(Long scanId);
    
    @Query("SELECT sr FROM ScanResult sr LEFT JOIN FETCH sr.scanTarget WHERE sr.scan.id = :scanId")
    List<ScanResult> findByScanIdWithTarget(@Param("scanId") Long scanId);
    
    List<ScanResult> findByScanTargetId(Long scanTargetId);
    
    List<ScanResult> findByProviderName(String providerName);
    
    @Query("SELECT AVG(sr.riskScore) FROM ScanResult sr WHERE sr.scan.id = :scanId")
    BigDecimal calculateAverageRiskScoreByScanId(@Param("scanId") Long scanId);
    
    @Query("SELECT COUNT(sr) FROM ScanResult sr WHERE sr.scan.id = :scanId")
    long countByScanId(@Param("scanId") Long scanId);
}

