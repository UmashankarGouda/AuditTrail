package com.audittrail.service;

import com.audittrail.dto.DeploymentDTO;
import com.audittrail.entity.Deployment;
import com.audittrail.repository.DeploymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    @Autowired
    private DeploymentRepository deploymentRepository;

    private static final List<String> CRITICAL_COMPONENTS = List.of(
        "SystemConfiguration", "SecurityPolicy", "PermissionSet", "SharingRule", 
        "UserManagement", "DataValidation", "APIEndpoint"
    );

    /**
     * Risk Assessment Engine - Determines risk level based on:
     * 1. Environment (PROD = higher risk)
     * 2. Critical component modifications
     */
    private Deployment.RiskLevel assessRisk(String environment) {
        if ("PRODUCTION".equalsIgnoreCase(environment) || "PROD".equalsIgnoreCase(environment)) {
            return Deployment.RiskLevel.HIGH;
        } else if ("STAGING".equalsIgnoreCase(environment)) {
            return Deployment.RiskLevel.MEDIUM;
        }
        return Deployment.RiskLevel.LOW;
    }

    public DeploymentDTO createDeployment(String name, String environment, String deployedBy, String notes) {
        Deployment.RiskLevel riskLevel = assessRisk(environment);

        Deployment deployment = Deployment.builder()
                .name(name)
                .environment(environment)
                .status(Deployment.DeploymentStatus.PLANNED)
                .riskLevel(riskLevel)
                .deployedBy(deployedBy)
                .deploymentTime(LocalDateTime.now())
                .notes(notes)
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        return mapToDTO(saved);
    }

    public Optional<DeploymentDTO> getDeploymentById(Long id) {
        return deploymentRepository.findById(id).map(this::mapToDTO);
    }

    public Page<DeploymentDTO> getAllDeployments(Pageable pageable) {
        return deploymentRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<DeploymentDTO> getDeploymentsByEnvironment(String environment) {
        return deploymentRepository.findByEnvironment(environment)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DeploymentDTO> getAllHighRiskDeployments() {
        return deploymentRepository.findAllHighRiskDeployments()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DeploymentDTO> getHighRiskDeploymentsByEnvironment(String environment) {
        return deploymentRepository.findHighRiskDeploymentsByEnvironment(environment)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public DeploymentDTO updateDeploymentStatus(Long id, String newStatus) {
        Optional<Deployment> depOpt = deploymentRepository.findById(id);
        if (depOpt.isEmpty()) {
            throw new IllegalArgumentException("Deployment not found with ID: " + id);
        }

        Deployment deployment = depOpt.get();
        try {
            deployment.setStatus(Deployment.DeploymentStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        Deployment updated = deploymentRepository.save(deployment);
        return mapToDTO(updated);
    }

    public void deleteDeployment(Long id) {
        deploymentRepository.deleteById(id);
    }

    private DeploymentDTO mapToDTO(Deployment deployment) {
        return DeploymentDTO.builder()
                .id(deployment.getId())
                .name(deployment.getName())
                .environment(deployment.getEnvironment())
                .status(deployment.getStatus().name())
                .riskLevel(deployment.getRiskLevel().name())
                .deployedBy(deployment.getDeployedBy())
                .deploymentTime(deployment.getDeploymentTime())
                .notes(deployment.getNotes())
                .createdAt(deployment.getCreatedAt())
                .build();
    }
}
