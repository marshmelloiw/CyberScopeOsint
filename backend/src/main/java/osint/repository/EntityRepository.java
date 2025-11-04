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
    
    @Query("SELECT e FROM Entity e WHERE e.user.id = :userId")
    List<Entity> findByUserId(@Param("userId") Long userId);
    
    // Note: riskLevel and riskScore are now transient fields, queries removed
    // If needed, these can be calculated from threat_events table
    
    @Query("SELECT COUNT(e) FROM Entity e WHERE e.entityType = :type")
    long countByEntityType(@Param("type") String type);
    
    @Query("SELECT COUNT(e) FROM Entity e WHERE e.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}

