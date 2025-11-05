package osint.repository;

import osint.model.ScanTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanTargetRepository extends JpaRepository<ScanTarget, Long> {
    
    List<ScanTarget> findByScanId(Long scanId);
    
    List<ScanTarget> findByTarget(String target);
}

