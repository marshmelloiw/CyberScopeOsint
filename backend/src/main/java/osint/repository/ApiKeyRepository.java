package osint.repository;

import osint.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    List<ApiKey> findByUserId(Long userId);
    
    List<ApiKey> findByStatus(String status);
    
    Optional<ApiKey> findByApiKey(String apiKey);
    
    @Query("SELECT a FROM ApiKey a WHERE a.userId = :userId AND a.status = :status")
    List<ApiKey> findByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT a FROM ApiKey a ORDER BY a.createdAt DESC")
    List<ApiKey> findAllOrderByCreatedAtDesc();
}

