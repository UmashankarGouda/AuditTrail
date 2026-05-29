package com.audittrail.service;

import com.audittrail.dto.ReleaseDTO;
import com.audittrail.entity.Release;
import com.audittrail.entity.ReleaseDeployment;
import com.audittrail.repository.ReleaseRepository;
import com.audittrail.repository.ReleaseDeploymentRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Release Service Tests")
class ReleaseServiceTest {

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ReleaseDeploymentRepository releaseDeploymentRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private MetadataChangeService metadataChangeService;

    @InjectMocks
    private ReleaseService releaseService;

    private Release mockRelease;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockRelease = Release.builder()
                .id(1L)
                .releaseName("Q2-2024 Release")
                .version("2.1.0")
                .status(Release.ReleaseStatus.PLANNED)
                .plannedDate(LocalDate.of(2024, 6, 15))
                .actualDate(null)
                .createdBy("release.manager")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create a new release successfully")
    void createRelease_shouldCreateNewRelease() {
        when(releaseRepository.save(any(Release.class))).thenReturn(mockRelease);

        ReleaseDTO result = releaseService.createRelease(
                "Q2-2024 Release", "2.1.0", LocalDate.of(2024, 6, 15), "release.manager"
        );

        assertNotNull(result);
        assertEquals("Q2-2024 Release", result.getReleaseName());
        assertEquals("2.1.0", result.getVersion());
        assertEquals("PLANNED", result.getStatus());
        verify(releaseRepository, times(1)).save(any(Release.class));
    }

    @Test
    @DisplayName("Should retrieve release by ID")
    void getReleaseById_shouldReturnRelease_whenFound() {
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));

        Optional<ReleaseDTO> result = releaseService.getReleaseById(1L);

        assertTrue(result.isPresent());
        assertEquals("Q2-2024 Release", result.get().getReleaseName());
        assertEquals("2.1.0", result.get().getVersion());
    }

    @Test
    @DisplayName("Should return empty Optional when release not found")
    void getReleaseById_shouldReturnEmpty_whenNotFound() {
        when(releaseRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<ReleaseDTO> result = releaseService.getReleaseById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should retrieve all releases with pagination")
    void getAllReleases_shouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Release> page = new PageImpl<>(Arrays.asList(mockRelease));

        when(releaseRepository.findAll(pageable)).thenReturn(page);

        Page<ReleaseDTO> result = releaseService.getAllReleases(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(releaseRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should retrieve releases by status")
    void getReleasesByStatus_shouldReturnFilteredReleases() {
        when(releaseRepository.findByStatus(Release.ReleaseStatus.PLANNED))
                .thenReturn(Arrays.asList(mockRelease));

        List<ReleaseDTO> result = releaseService.getReleasesByStatus("PLANNED");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PLANNED", result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should update release status successfully")
    void updateReleaseStatus_shouldUpdateStatus_whenValid() {
        Release updatedRelease = Release.builder()
                .id(1L)
                .releaseName("Q2-2024 Release")
                .version("2.1.0")
                .status(Release.ReleaseStatus.IN_PROGRESS)
                .plannedDate(LocalDate.of(2024, 6, 15))
                .createdBy("release.manager")
                .createdAt(LocalDateTime.now())
                .build();

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));
        when(releaseRepository.save(any(Release.class))).thenReturn(updatedRelease);

        ReleaseDTO result = releaseService.updateReleaseStatus(1L, "IN_PROGRESS");

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(releaseRepository, times(1)).save(any(Release.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid release status")
    void updateReleaseStatus_shouldThrowException_whenStatusInvalid() {
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));

        assertThrows(IllegalArgumentException.class, () -> {
            releaseService.updateReleaseStatus(1L, "INVALID_STATUS");
        });
    }

    @Test
    @DisplayName("Should delete release successfully")
    void deleteRelease_shouldDelete_whenReleaseExists() {
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));
        when(releaseDeploymentRepository.findByReleaseId(1L)).thenReturn(Arrays.asList());

        releaseService.deleteRelease(1L);

        verify(releaseDeploymentRepository, times(1)).deleteByReleaseId(1L);
        verify(releaseRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should retrieve release summary with aggregation")
    void getReleaseSummary_shouldAggregateDeploymentAndChangeData() {
        ReleaseDeployment dep1 = ReleaseDeployment.builder()
                .id(1L)
                .releaseId(1L)
                .deploymentId(1L)
                .build();

        ReleaseDeployment dep2 = ReleaseDeployment.builder()
                .id(2L)
                .releaseId(1L)
                .deploymentId(2L)
                .build();

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));
        when(releaseDeploymentRepository.findByReleaseId(1L))
                .thenReturn(Arrays.asList(dep1, dep2));
        when(metadataChangeService.getChangeCountByDeploymentId(1L)).thenReturn(5L);
        when(metadataChangeService.getChangeCountByDeploymentId(2L)).thenReturn(3L);

        ReleaseService.ReleaseSummaryDTO summary = releaseService.getReleaseSummary(1L);

        assertNotNull(summary);
        assertEquals(1L, summary.releaseId);
        assertEquals("Q2-2024 Release", summary.releaseName);
        assertEquals(2L, summary.totalDeployments);
        assertEquals(8L, summary.totalMetadataChanges);
    }

    @Test
    @DisplayName("Should throw exception when release not found in summary")
    void getReleaseSummary_shouldThrowException_whenReleaseNotFound() {
        when(releaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            releaseService.getReleaseSummary(999L);
        });
    }

    @Test
    @DisplayName("Should assign deployment to release successfully")
    void assignDeploymentToRelease_shouldCreateLink() {
        ReleaseDeployment linkEntity = ReleaseDeployment.builder()
                .id(1L)
                .releaseId(1L)
                .deploymentId(1L)
                .build();

        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));        when(deploymentRepository.existsById(1L)).thenReturn(true);
        when(releaseDeploymentRepository.existsByReleaseIdAndDeploymentId(1L, 1L)).thenReturn(false);        when(releaseDeploymentRepository.save(any(ReleaseDeployment.class)))
                .thenReturn(linkEntity);

        ReleaseDTO result = releaseService.assignDeploymentToRelease(1L, 1L);

        assertNotNull(result);
        assertEquals("Q2-2024 Release", result.getReleaseName());
        verify(releaseDeploymentRepository, times(1)).save(any(ReleaseDeployment.class));
    }

    @Test
    @DisplayName("Should remove deployment from release successfully")
    void removeDeploymentFromRelease_shouldDeleteLink() {
        when(releaseRepository.findById(1L)).thenReturn(Optional.of(mockRelease));

        ReleaseDTO result = releaseService.removeDeploymentFromRelease(1L, 1L);

        assertNotNull(result);
        verify(releaseDeploymentRepository, times(1)).deleteByReleaseIdAndDeploymentId(1L, 1L);
    }
}
