package osint.repository;

import osint.model.ScanProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository - Disabled: Scan tables don't exist
public interface ScanProviderRepository extends JpaRepository<ScanProvider, Long> {
    
    List<ScanProvider> findByScanId(Long scanId);
    
    List<ScanProvider> findByProviderName(String providerName);
}

