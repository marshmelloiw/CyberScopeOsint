package osint.repository;

import osint.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScanRepository extends JpaRepository<Scan, Long> {
    
    Optional<Scan> findByScanId(String scanId);
    
    List<Scan> findByUserId(Long userId);
    
    List<Scan> findByStatus(String status);
    
    List<Scan> findByType(String type);
    
    @Query("SELECT s FROM Scan s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Scan> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(s) FROM Scan s WHERE s.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(s) FROM Scan s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    long countByStatusIn(Collection<String> statuses);
}

