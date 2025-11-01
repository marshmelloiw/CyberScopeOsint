package osint.repository;

import osint.model.EntityFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityFindingRepository extends JpaRepository<EntityFinding, Long> {
    
    List<EntityFinding> findByEntityId(Long entityId);
    
    List<EntityFinding> findByScanId(Long scanId);
    
    List<EntityFinding> findByFindingType(String findingType);
    
    List<EntityFinding> findBySeverity(String severity);
    
    List<EntityFinding> findByStatus(String status);
    
    @Query("SELECT ef FROM EntityFinding ef WHERE ef.entity.id = :entityId AND ef.status = 'ACTIVE' ORDER BY ef.severity DESC, ef.discoveredAt DESC")
    List<EntityFinding> findActiveFindingsByEntityId(@Param("entityId") Long entityId);
    
    @Query("SELECT COUNT(ef) FROM EntityFinding ef WHERE ef.status = 'ACTIVE'")
    long countActiveFindings();
}

