package com.audittrail.service;

import com.audittrail.dto.MetadataChangeDTO;
import com.audittrail.entity.MetadataChange;
import com.audittrail.repository.MetadataChangeRepository;
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

@DisplayName("Metadata Change Service Tests")
class MetadataChangeServiceTest {

    @Mock
    private MetadataChangeRepository metadataChangeRepository;

    @InjectMocks
    private MetadataChangeService metadataChangeService;

    private MetadataChange mockChange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockChange = MetadataChange.builder()
                .id(1L)
                .deploymentId(1L)
                .componentName("AccountTrigger")
                .componentType("ApexClass")
                .changeType(MetadataChange.ChangeType.MODIFIED)
                .oldValue("old code")
                .newValue("new code")
                .changedBy("john.dev")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should log metadata change successfully")
    void logMetadataChange_shouldCreateRecord() {
        when(metadataChangeRepository.save(any(MetadataChange.class))).thenReturn(mockChange);

        MetadataChangeDTO result = metadataChangeService.logMetadataChange(
                1L, "AccountTrigger", "ApexClass", "MODIFIED", "old code", "new code", "john.dev"
        );

        assertNotNull(result);
        assertEquals("AccountTrigger", result.getComponentName());
        assertEquals("MODIFIED", result.getChangeType());
        verify(metadataChangeRepository, times(1)).save(any(MetadataChange.class));
    }

    @Test
    @DisplayName("Should retrieve changes by deployment ID")
    void getChangesByDeploymentId_shouldReturnAllChanges() {
        when(metadataChangeRepository.findByDeploymentId(1L))
                .thenReturn(Arrays.asList(mockChange));

        List<MetadataChangeDTO> result = metadataChangeService.getChangesByDeploymentId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AccountTrigger", result.get(0).getComponentName());
    }

    @Test
    @DisplayName("Should retrieve changes by deployment ID with pagination")
    void getChangesByDeploymentIdPaged_shouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MetadataChange> page = new PageImpl<>(Arrays.asList(mockChange));

        when(metadataChangeRepository.findByDeploymentId(1L, pageable))
                .thenReturn(page);

        Page<MetadataChangeDTO> result = metadataChangeService.getChangesByDeploymentIdPaged(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should retrieve component history")
    void getComponentHistory_shouldReturnAllChangesForComponent() {
        when(metadataChangeRepository.findByComponentName("AccountTrigger"))
                .thenReturn(Arrays.asList(mockChange));

        List<MetadataChangeDTO> result = metadataChangeService.getComponentHistory("AccountTrigger");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AccountTrigger", result.get(0).getComponentName());
    }

    @Test
    @DisplayName("Should skip invalid rows in batch import")
    void logMetadataChange_shouldHandleInvalidChangeType() {
        MetadataChange invalidChange = MetadataChange.builder()
                .id(2L)
                .deploymentId(1L)
                .componentName("Component")
                .componentType("Type")
                .changeType(MetadataChange.ChangeType.MODIFIED) // Default fallback
                .changedBy("user")
                .changedAt(LocalDateTime.now())
                .build();

        when(metadataChangeRepository.save(any(MetadataChange.class))).thenReturn(invalidChange);

        MetadataChangeDTO result = metadataChangeService.logMetadataChange(
                1L, "Component", "Type", "INVALID_TYPE", "", "", "user"
        );

        assertNotNull(result);
        assertEquals("MODIFIED", result.getChangeType()); // Should default to MODIFIED
    }

    @Test
    @DisplayName("Should count changes by deployment ID")
    void getChangeCountByDeploymentId_shouldReturnCount() {
        when(metadataChangeRepository.countByDeploymentId(1L)).thenReturn(5L);

        long result = metadataChangeService.getChangeCountByDeploymentId(1L);

        assertEquals(5L, result);
    }
}
