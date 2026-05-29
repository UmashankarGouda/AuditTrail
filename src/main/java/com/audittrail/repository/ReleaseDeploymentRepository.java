package com.audittrail.repository;

import com.audittrail.entity.ReleaseDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReleaseDeploymentRepository extends JpaRepository<ReleaseDeployment, Long> {
    List<ReleaseDeployment> findByReleaseId(Long releaseId);
    
    List<ReleaseDeployment> findByDeploymentId(Long deploymentId);
    
    boolean existsByReleaseIdAndDeploymentId(Long releaseId, Long deploymentId);
    
    void deleteByReleaseIdAndDeploymentId(Long releaseId, Long deploymentId);
    
    void deleteByReleaseId(Long releaseId);
}
