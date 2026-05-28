package com.audittrail.repository;

import com.audittrail.entity.Deployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Page<Deployment> findAll(Pageable pageable);
    
    List<Deployment> findByEnvironment(String environment);
    
    @Query("SELECT d FROM Deployment d WHERE d.riskLevel = 'HIGH'")
    List<Deployment> findAllHighRiskDeployments();
    
    @Query("SELECT d FROM Deployment d WHERE d.environment = ?1 AND d.riskLevel = 'HIGH'")
    List<Deployment> findHighRiskDeploymentsByEnvironment(String environment);
    
    long countByEnvironment(String environment);
}
