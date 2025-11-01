package osint.repository;

import osint.model.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {
    
    Optional<Entity> findByEntityTypeAndEntityValue(String entityType, String entityValue);
    
    List<Entity> findByEntityType(String entityType);
    
    List<Entity> findByRiskLevel(String riskLevel);
    
    @Query("SELECT e FROM Entity e WHERE e.riskScore >= :minScore ORDER BY e.riskScore DESC")
    List<Entity> findByRiskScoreGreaterThanEqual(@Param("minScore") java.math.BigDecimal minScore);
    
    @Query("SELECT COUNT(e) FROM Entity e WHERE e.entityType = :type")
    long countByEntityType(@Param("type") String type);
}

