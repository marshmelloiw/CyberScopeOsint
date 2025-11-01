package osint.repository;

import osint.model.ScanProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanProviderRepository extends JpaRepository<ScanProvider, Long> {
    
    List<ScanProvider> findByScanId(Long scanId);
    
    List<ScanProvider> findByProviderName(String providerName);
}

