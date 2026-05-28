package com.audittrail.repository;

import com.audittrail.entity.MetadataChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MetadataChangeRepository extends JpaRepository<MetadataChange, Long> {
    List<MetadataChange> findByDeploymentId(Long deploymentId);
    
    Page<MetadataChange> findByDeploymentId(Long deploymentId, Pageable pageable);
    
    List<MetadataChange> findByComponentName(String componentName);
    
    Page<MetadataChange> findByComponentName(String componentName, Pageable pageable);
    
    long countByDeploymentId(Long deploymentId);
}
