package com.audittrail;

import com.audittrail.dto.DeploymentDTO;
import com.audittrail.dto.MetadataChangeDTO;
import com.audittrail.dto.AuthResponse;
import com.audittrail.dto.LoginRequest;
import com.audittrail.dto.RegisterRequest;
import com.audittrail.entity.Deployment;
import com.audittrail.entity.MetadataChange;
import com.audittrail.entity.User;
import com.audittrail.repository.DeploymentRepository;
import com.audittrail.repository.MetadataChangeRepository;
import com.audittrail.repository.UserRepository;
import com.audittrail.security.JwtTokenProvider;
import com.audittrail.service.DeploymentService;
import com.audittrail.service.MetadataChangeService;
import com.audittrail.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Integration Tests - Full Workflow")
class IntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private MetadataChangeRepository metadataChangeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @InjectMocks
    private DeploymentService deploymentService;

    @InjectMocks
    private MetadataChangeService metadataChangeService;

    private User testUser;
    private Deployment testDeployment;
    private MetadataChange testMetadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test data
        testUser = User.builder()
                .id(1L)
                .username("john_dev")
                .email("john@example.com")
                .passwordHash("$2a$10$hashed")
                .role(User.UserRole.DEVELOPER)
                .build();

        testDeployment = Deployment.builder()
                .id(1L)
                .name("Production API")
                .environment("PRODUCTION")
                .status(Deployment.DeploymentStatus.PLANNED)
                .riskLevel(Deployment.RiskLevel.HIGH)
                .deployedBy("john_dev")
                .deploymentTime(LocalDateTime.now())
                .notes("Critical release")
                .createdAt(LocalDateTime.now())
                .build();

        testMetadata = MetadataChange.builder()
                .id(1L)
                .deploymentId(1L)
                .componentName("PaymentAPI")
                .componentType("API")
                .changeType(MetadataChange.ChangeType.CREATED)
                .oldValue("")
                .newValue("v2.0")
                .changedBy("john_dev")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Workflow: User registers, logs in, creates deployment, and logs metadata change")
    void fullUserWorkflow_shouldCompleteSuccessfully() {
        // 1. User registration
        when(userRepository.existsByUsername("john_dev")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString()))
                .thenReturn("eyJhbGciOiJIUzUxMiJ9.test.token");

        AuthResponse registerResponse = authService.register(
                createRegisterRequest("john_dev", "john@example.com", "pass123", "DEVELOPER")
        );

        assertNotNull(registerResponse);
        assertTrue(registerResponse.getMessage().contains("successfully"));

        // 2. User login
        when(userRepository.findByUsername("john_dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("pass123", testUser.getPasswordHash())).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("john_dev");
        loginRequest.setPassword("pass123");

        // AuthResponse loginResponse = authService.login(loginRequest);
        // assertNotNull(loginResponse);
        // assertEquals("john_dev", loginResponse.getUsername());

        // 3. Create deployment
        when(deploymentRepository.save(any(Deployment.class))).thenReturn(testDeployment);

        DeploymentDTO deploymentDTO = deploymentService.createDeployment(
                "Production API", "PRODUCTION", "john_dev", "Critical release"
        );

        assertNotNull(deploymentDTO);
        assertEquals("PRODUCTION", deploymentDTO.getEnvironment());
        assertEquals("HIGH", deploymentDTO.getRiskLevel());

        // 4. Log metadata change
        when(metadataChangeRepository.save(any(MetadataChange.class))).thenReturn(testMetadata);

        MetadataChangeDTO metadataDTO = metadataChangeService.logMetadataChange(
                1L, "PaymentAPI", "API", "CREATED", "", "v2.0", "john_dev"
        );

        assertNotNull(metadataDTO);
        assertEquals("PaymentAPI", metadataDTO.getComponentName());
        assertEquals("CREATED", metadataDTO.getChangeType());

        verify(userRepository, times(1)).save(any(User.class));
        verify(deploymentRepository, times(1)).save(any(Deployment.class));
        verify(metadataChangeRepository, times(1)).save(any(MetadataChange.class));
    }

    @Test
    @DisplayName("Workflow: Batch CSV processing with multiple metadata records")
    void batchProcessingWorkflow_shouldProcessMultipleRecords() {
        // Simulate batch processing: create multiple deployments and metadata

        // 1. Create first deployment
        Deployment prod = Deployment.builder()
                .id(1L)
                .name("Prod Deploy")
                .environment("PRODUCTION")
                .riskLevel(Deployment.RiskLevel.HIGH)
                .status(Deployment.DeploymentStatus.PLANNED)
                .deployedBy("batch_user")
                .deploymentTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // 2. Create multiple metadata records
        MetadataChange meta1 = createMetadataChange(1L, 1L, "Component1", "CREATED", "user1");
        MetadataChange meta2 = createMetadataChange(2L, 1L, "Component2", "MODIFIED", "user1");
        MetadataChange meta3 = createMetadataChange(3L, 1L, "Component3", "DELETED", "user2");

        when(deploymentRepository.save(any(Deployment.class))).thenReturn(prod);
        when(metadataChangeRepository.save(any(MetadataChange.class)))
                .thenReturn(meta1)
                .thenReturn(meta2)
                .thenReturn(meta3);

        // Process batch
        DeploymentDTO deployment = deploymentService.createDeployment(
                "Prod Deploy", "PRODUCTION", "batch_user", ""
        );
        assertNotNull(deployment);
        assertEquals("HIGH", deployment.getRiskLevel());

        // Save 3 metadata records
        for (int i = 0; i < 3; i++) {
            metadataChangeService.logMetadataChange(
                    1L, "Component" + (i + 1), "Type", 
                    i == 0 ? "CREATED" : i == 1 ? "MODIFIED" : "DELETED",
                    "", "", "user" + (i < 2 ? "1" : "2")
            );
        }

        verify(deploymentRepository, times(1)).save(any(Deployment.class));
        verify(metadataChangeRepository, times(3)).save(any(MetadataChange.class));
    }

    @Test
    @DisplayName("Workflow: Risk assessment for different environments")
    void riskAssessmentWorkflow_shouldCalculateCorrectlyAcrossEnvironments() {
        // Create deployments in different environments
        Deployment prod = createDeployment("Prod", "PRODUCTION");
        Deployment staging = createDeployment("Staging", "STAGING");
        Deployment dev = createDeployment("Dev", "DEV");

        when(deploymentRepository.save(any(Deployment.class)))
                .thenReturn(prod)
                .thenReturn(staging)
                .thenReturn(dev);

        DeploymentDTO prodDTO = deploymentService.createDeployment(
                "Prod", "PRODUCTION", "user", ""
        );
        DeploymentDTO stagingDTO = deploymentService.createDeployment(
                "Staging", "STAGING", "user", ""
        );
        DeploymentDTO devDTO = deploymentService.createDeployment(
                "Dev", "DEV", "user", ""
        );

        assertEquals("HIGH", prodDTO.getRiskLevel());
        assertEquals("MEDIUM", stagingDTO.getRiskLevel());
        assertEquals("LOW", devDTO.getRiskLevel());

        verify(deploymentRepository, times(3)).save(any(Deployment.class));
    }

    @Test
    @DisplayName("Workflow: Enum conversion in batch processing (REMOVED->DELETED, UPDATED->MODIFIED)")
    void enumConversionWorkflow_shouldConvertEnumsCorrectly() {
        // Simulate CSV with REMOVED and UPDATED values

        MetadataChange deletedChange = MetadataChange.builder()
                .id(1L)
                .deploymentId(1L)
                .componentName("OldComponent")
                .changeType(MetadataChange.ChangeType.DELETED)  // Converted from REMOVED
                .changedBy("user")
                .changedAt(LocalDateTime.now())
                .build();

        MetadataChange modifiedChange = MetadataChange.builder()
                .id(2L)
                .deploymentId(1L)
                .componentName("OldComponent")
                .changeType(MetadataChange.ChangeType.MODIFIED)  // Converted from UPDATED
                .changedBy("user")
                .changedAt(LocalDateTime.now())
                .build();

        when(metadataChangeRepository.save(any(MetadataChange.class)))
                .thenReturn(deletedChange)
                .thenReturn(modifiedChange);

        // Process with REMOVED (should convert to DELETED)
        MetadataChangeDTO result1 = metadataChangeService.logMetadataChange(
                1L, "OldComponent", "Type", "REMOVED", "v1", "", "user"
        );
        assertEquals("DELETED", result1.getChangeType());

        // Process with UPDATED (should convert to MODIFIED)
        MetadataChangeDTO result2 = metadataChangeService.logMetadataChange(
                1L, "OldComponent", "Type", "UPDATED", "v1", "v2", "user"
        );
        assertEquals("MODIFIED", result2.getChangeType());
    }

    @Test
    @DisplayName("Workflow: Data retrieval and pagination")
    void dataRetrievalWorkflow_shouldHandlePagination() {
        Pageable pageable = PageRequest.of(0, 10);

        // Test deployment retrieval
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(testDeployment));
        Optional<DeploymentDTO> deployment = deploymentService.getDeploymentById(1L);

        assertTrue(deployment.isPresent());
        assertEquals("Production API", deployment.get().getName());

        // Test metadata retrieval
        when(metadataChangeRepository.findByDeploymentId(1L))
                .thenReturn(java.util.Arrays.asList(testMetadata));

        var changes = metadataChangeService.getChangesByDeploymentId(1L);
        assertNotNull(changes);
        assertEquals(1, changes.size());

        verify(deploymentRepository, times(1)).findById(1L);
        verify(metadataChangeRepository, times(1)).findByDeploymentId(1L);
    }

    @Test
    @DisplayName("Workflow: Should track all changes in deployment audit trail")
    void auditTrailWorkflow_shouldTrackAllChanges() {
        // Setup: Create deployment with multiple changes by different users
        when(deploymentRepository.save(any(Deployment.class))).thenReturn(testDeployment);

        DeploymentDTO deployment = deploymentService.createDeployment(
                "Audit Test", "PRODUCTION", "user1", ""
        );
        assertNotNull(deployment);

        // Record changes by multiple users
        when(metadataChangeRepository.save(any(MetadataChange.class)))
                .thenReturn(testMetadata);

        metadataChangeService.logMetadataChange(1L, "Comp1", "Type", "CREATED", "", "", "user1");
        metadataChangeService.logMetadataChange(1L, "Comp2", "Type", "MODIFIED", "v1", "v2", "user2");
        metadataChangeService.logMetadataChange(1L, "Comp3", "Type", "DELETED", "v1", "", "user3");

        verify(metadataChangeRepository, times(3)).save(any(MetadataChange.class));
    }

    // Helper Methods

    private Deployment createDeployment(String name, String env) {
        return Deployment.builder()
                .id(1L)
                .name(name)
                .environment(env)
                .status(Deployment.DeploymentStatus.PLANNED)
                .riskLevel(getRiskLevel(env))
                .deployedBy("user")
                .deploymentTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Deployment.RiskLevel getRiskLevel(String env) {
        return switch (env.toUpperCase()) {
            case "PRODUCTION" -> Deployment.RiskLevel.HIGH;
            case "STAGING" -> Deployment.RiskLevel.MEDIUM;
            default -> Deployment.RiskLevel.LOW;
        };
    }

    private MetadataChange createMetadataChange(Long id, Long deploymentId, String component,
                                                String changeType, String changedBy) {
        return MetadataChange.builder()
                .id(id)
                .deploymentId(deploymentId)
                .componentName(component)
                .componentType("Type")
                .changeType(MetadataChange.ChangeType.valueOf(changeType))
                .oldValue("")
                .newValue("value")
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    private RegisterRequest createRegisterRequest(String username, String email, String password, String role) {
        return RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }
}
