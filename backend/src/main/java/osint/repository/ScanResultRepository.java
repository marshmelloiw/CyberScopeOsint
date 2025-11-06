package osint.repository;

import osint.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

@Repository
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

    long countByFindingsCountGreaterThan(int findingsCount);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query(value = "SELECT AVG(CAST(sr.risk_score AS DOUBLE PRECISION)) FROM scan_results sr WHERE sr.risk_score IS NOT NULL", nativeQuery = true)
    Double findAverageRiskScore();

    List<ScanResult> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

