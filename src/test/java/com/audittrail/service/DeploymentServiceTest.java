package com.audittrail.service;

import com.audittrail.dto.DeploymentDTO;
import com.audittrail.entity.Deployment;
import com.audittrail.repository.DeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Deployment Service Tests")
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @InjectMocks
    private DeploymentService deploymentService;

    private Deployment mockDeployment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockDeployment = Deployment.builder()
                .id(1L)
                .name("Production Deployment")
                .environment("PRODUCTION")
                .status(Deployment.DeploymentStatus.PLANNED)
                .riskLevel(Deployment.RiskLevel.HIGH)
                .deployedBy("john.dev")
                .deploymentTime(LocalDateTime.now())
                .notes("Critical production release")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create deployment with HIGH risk for PROD environment")
    void createDeployment_shouldSetRiskLevelHigh_whenProductionEnvironment() {
        when(deploymentRepository.save(any(Deployment.class))).thenReturn(mockDeployment);

        DeploymentDTO result = deploymentService.createDeployment(
                "Prod Deployment", "PRODUCTION", "john.dev", "Critical release"
        );

        assertNotNull(result);
        assertEquals("HIGH", result.getRiskLevel());
        assertEquals("PRODUCTION", result.getEnvironment());
        verify(deploymentRepository, times(1)).save(any(Deployment.class));
    }

    @Test
    @DisplayName("Should create deployment with LOW risk for DEV environment")
    void createDeployment_shouldSetRiskLevelLow_whenDevEnvironment() {
        Deployment devDeployment = Deployment.builder()
                .id(2L)
                .name("Dev Deployment")
                .environment("DEV")
                .status(Deployment.DeploymentStatus.PLANNED)
                .riskLevel(Deployment.RiskLevel.LOW)
                .deployedBy("jane.dev")
                .deploymentTime(LocalDateTime.now())
                .notes("Dev testing")
                .createdAt(LocalDateTime.now())
                .build();

        when(deploymentRepository.save(any(Deployment.class))).thenReturn(devDeployment);

        DeploymentDTO result = deploymentService.createDeployment(
                "Dev Deployment", "DEV", "jane.dev", "Dev testing"
        );

        assertNotNull(result);
        assertEquals("LOW", result.getRiskLevel());
        assertEquals("DEV", result.getEnvironment());
    }

    @Test
    @DisplayName("Should retrieve deployment by ID successfully")
    void getDeploymentById_shouldReturnDeployment_whenFound() {
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(mockDeployment));

        Optional<DeploymentDTO> result = deploymentService.getDeploymentById(1L);

        assertTrue(result.isPresent());
        assertEquals("Production Deployment", result.get().getName());
        verify(deploymentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deployment not found")
    void getDeploymentById_shouldThrowException_whenNotFound() {
        when(deploymentRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<DeploymentDTO> result = deploymentService.getDeploymentById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get all deployments paginated")
    void getAllDeployments_shouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Deployment> page = new PageImpl<>(Arrays.asList(mockDeployment));

        when(deploymentRepository.findAll(pageable)).thenReturn(page);

        Page<DeploymentDTO> result = deploymentService.getAllDeployments(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(deploymentRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should get deployments by environment")
    void getDeploymentsByEnvironment_shouldReturnFilteredResults() {
        when(deploymentRepository.findByEnvironment("PRODUCTION"))
                .thenReturn(Arrays.asList(mockDeployment));

        List<DeploymentDTO> result = deploymentService.getDeploymentsByEnvironment("PRODUCTION");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRODUCTION", result.get(0).getEnvironment());
    }

    @Test
    @DisplayName("Should get all high-risk deployments")
    void getAllHighRiskDeployments_shouldReturnOnlyHighRiskDeployments() {
        when(deploymentRepository.findAllHighRiskDeployments())
                .thenReturn(Arrays.asList(mockDeployment));

        List<DeploymentDTO> result = deploymentService.getAllHighRiskDeployments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("HIGH", result.get(0).getRiskLevel());
    }

    @Test
    @DisplayName("Should update deployment status successfully")
    void updateDeploymentStatus_shouldUpdateStatus() {
        mockDeployment.setStatus(Deployment.DeploymentStatus.IN_PROGRESS);
        
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(mockDeployment));
        when(deploymentRepository.save(any(Deployment.class))).thenReturn(mockDeployment);

        DeploymentDTO result = deploymentService.updateDeploymentStatus(1L, "IN_PROGRESS");

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    @DisplayName("Should throw exception for invalid status")
    void updateDeploymentStatus_shouldThrowException_whenInvalidStatus() {
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(mockDeployment));

        assertThrows(IllegalArgumentException.class, 
                () -> deploymentService.updateDeploymentStatus(1L, "INVALID_STATUS"));
    }
}
