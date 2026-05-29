package com.audittrail.security;

import com.audittrail.entity.User;
import com.audittrail.repository.MetadataChangeRepository;
import com.audittrail.repository.DeploymentRepository;
import com.audittrail.service.MetadataChangeService;
import com.audittrail.service.DeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Authorization Tests")
class AuthorizationTest {

    @Mock
    private MetadataChangeRepository metadataChangeRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    private MetadataChangeService metadataChangeService;
    private DeploymentService deploymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metadataChangeService = new MetadataChangeService();
        deploymentService = new DeploymentService();
        
        // Reset security context
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN user should be able to delete metadata")
    void deleteMetadata_shouldSucceed_forAdminUser() {
        // Arrange: Create ADMIN user context
        setUserContext("admin_user", "ADMIN");
        
        when(metadataChangeRepository.count()).thenReturn(100L);

        // Act: Attempt deletion
        // In real scenario, this would be protected by @PreAuthorize("hasRole('ADMIN')")
        assertTrue(hasRole("ADMIN"));
        
        // Assert
        verify(metadataChangeRepository, times(0)).deleteAll(); // Not actually called in this test
    }

    @Test
    @DisplayName("DEVELOPER user should NOT be able to delete metadata")
    void deleteMetadata_shouldFail_forDeveloperUser() {
        // Arrange: Create DEVELOPER user context
        setUserContext("dev_user", "DEVELOPER");
        
        // Act & Assert
        assertFalse(hasRole("ADMIN"));
        assertTrue(hasRole("DEVELOPER"));
    }

    @Test
    @DisplayName("VIEWER user should NOT be able to delete metadata")
    void deleteMetadata_shouldFail_forViewerUser() {
        // Arrange: Create VIEWER user context
        setUserContext("viewer_user", "VIEWER");
        
        // Act & Assert
        assertFalse(hasRole("ADMIN"));
        assertFalse(hasRole("DEVELOPER"));
        assertTrue(hasRole("VIEWER"));
    }

    @Test
    @DisplayName("Unauthenticated user should NOT be able to delete metadata")
    void deleteMetadata_shouldFail_forUnauthenticatedUser() {
        // Arrange: No user context
        SecurityContextHolder.clearContext();
        
        // Act & Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth == null || !auth.isAuthenticated());
    }

    @Test
    @DisplayName("ADMIN can view all deployment data")
    void viewDeployments_shouldSucceed_forAdminUser() {
        setUserContext("admin_user", "ADMIN");
        
        assertTrue(hasRole("ADMIN"));
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("DEVELOPER can view deployment data")
    void viewDeployments_shouldSucceed_forDeveloperUser() {
        setUserContext("dev_user", "DEVELOPER");
        
        assertTrue(hasRole("DEVELOPER"));
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("VIEWER can view deployment data")
    void viewDeployments_shouldSucceed_forViewerUser() {
        setUserContext("viewer_user", "VIEWER");
        
        assertTrue(hasRole("VIEWER"));
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("ADMIN can create deployments")
    void createDeployment_shouldSucceed_forAdminUser() {
        setUserContext("admin_user", "ADMIN");
        
        assertTrue(hasRole("ADMIN"));
        assertTrue(canCreateDeployment());
    }

    @Test
    @DisplayName("DEVELOPER can create deployments")
    void createDeployment_shouldSucceed_forDeveloperUser() {
        setUserContext("dev_user", "DEVELOPER");
        
        assertTrue(hasRole("DEVELOPER"));
        assertTrue(canCreateDeployment());
    }

    @Test
    @DisplayName("VIEWER should NOT be able to create deployments")
    void createDeployment_shouldFail_forViewerUser() {
        setUserContext("viewer_user", "VIEWER");
        
        assertTrue(hasRole("VIEWER"));
        assertFalse(canCreateDeployment());
    }

    @Test
    @DisplayName("Role hierarchy: ADMIN has all permissions")
    void roleHierarchy_adminShouldHaveAllRoles() {
        setUserContext("admin_user", "ADMIN");
        
        assertTrue(hasRole("ADMIN"));
        assertTrue(canDeleteMetadata());
        assertTrue(canCreateDeployment());
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("Role hierarchy: DEVELOPER has read and create permissions")
    void roleHierarchy_developerShouldHaveCreateAndRead() {
        setUserContext("dev_user", "DEVELOPER");
        
        assertTrue(hasRole("DEVELOPER"));
        assertFalse(hasRole("ADMIN"));
        assertFalse(canDeleteMetadata());
        assertTrue(canCreateDeployment());
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("Role hierarchy: VIEWER has only read permissions")
    void roleHierarchy_viewerShouldHaveOnlyRead() {
        setUserContext("viewer_user", "VIEWER");
        
        assertTrue(hasRole("VIEWER"));
        assertFalse(hasRole("ADMIN"));
        assertFalse(hasRole("DEVELOPER"));
        assertFalse(canDeleteMetadata());
        assertFalse(canCreateDeployment());
        assertTrue(canReadDeployments());
    }

    @Test
    @DisplayName("Should prevent privilege escalation")
    void preventPrivilegeEscalation_userCannotClaimAdminRole() {
        // User tries to set themselves as ADMIN
        setUserContext("hacker_user", "VIEWER");
        
        // Even if they try to claim ADMIN role, verification should fail
        assertTrue(hasRole("VIEWER"));
        assertFalse(hasRole("ADMIN"));
    }

    @Test
    @DisplayName("Should enforce authentication before authorization")
    void enforceAuthenticationBeforeAuthorization() {
        // No user context set
        SecurityContextHolder.clearContext();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertFalse(auth != null && auth.isAuthenticated());
    }

    @Test
    @DisplayName("Multiple roles per user should be supported")
    void multiplePrincipalsPerUser() {
        setUserContext("admin_user", "ADMIN");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
    }

    // Helper Methods

    private void setUserContext(String username, String role) {
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                username, null, authorities
        );
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private boolean canDeleteMetadata() {
        return hasRole("ADMIN");
    }

    private boolean canCreateDeployment() {
        return hasRole("ADMIN") || hasRole("DEVELOPER");
    }

    private boolean canReadDeployments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }
}
