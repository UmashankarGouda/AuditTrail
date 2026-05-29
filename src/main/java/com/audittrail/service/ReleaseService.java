package com.audittrail.service;

import com.audittrail.dto.ReleaseDTO;
import com.audittrail.entity.Release;
import com.audittrail.entity.ReleaseDeployment;
import com.audittrail.repository.ReleaseRepository;
import com.audittrail.repository.ReleaseDeploymentRepository;
import com.audittrail.repository.DeploymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReleaseService {

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private ReleaseDeploymentRepository releaseDeploymentRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private MetadataChangeService metadataChangeService;

    public ReleaseDTO createRelease(String releaseName, String version, LocalDate plannedDate, String createdBy) {
        if (releaseRepository.existsByVersion(version)) {
            throw new IllegalArgumentException("Version already exists: " + version);
        }

        Release release = Release.builder()
                .releaseName(releaseName)
                .version(version)
                .status(Release.ReleaseStatus.PLANNED)
                .plannedDate(plannedDate)
                .createdBy(createdBy)
                .build();

        Release saved = releaseRepository.save(release);
        return mapToDTO(saved);
    }

    public Optional<ReleaseDTO> getReleaseById(Long id) {
        return releaseRepository.findById(id).map(this::mapToDTO);
    }

    public Page<ReleaseDTO> getAllReleases(Pageable pageable) {
        return releaseRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<ReleaseDTO> getReleasesByStatus(String status) {
        Release.ReleaseStatus releaseStatus;
        try {
            releaseStatus = Release.ReleaseStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        return releaseRepository.findByStatus(releaseStatus)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ReleaseDTO assignDeploymentToRelease(Long releaseId, Long deploymentId) {
        Optional<Release> releaseOpt = releaseRepository.findById(releaseId);
        if (releaseOpt.isEmpty()) {
            throw new IllegalArgumentException("Release not found with ID: " + releaseId);
        }

        Release release = releaseOpt.get();

        // Check if release is in a closed state
        if (release.getStatus() == Release.ReleaseStatus.ROLLED_BACK) {
            throw new IllegalStateException("Cannot assign deployment to rolled back release");
        }

        // Check if deployment exists
        if (!deploymentRepository.existsById(deploymentId)) {
            throw new IllegalArgumentException("Deployment not found with ID: " + deploymentId);
        }

        // Check if already assigned
        if (releaseDeploymentRepository.existsByReleaseIdAndDeploymentId(releaseId, deploymentId)) {
            throw new IllegalStateException("Deployment already assigned to this release");
        }

        ReleaseDeployment rd = ReleaseDeployment.builder()
                .releaseId(releaseId)
                .deploymentId(deploymentId)
                .build();

        releaseDeploymentRepository.save(rd);
        return mapToDTO(release);
    }

    public ReleaseDTO removeDeploymentFromRelease(Long releaseId, Long deploymentId) {
        Optional<Release> releaseOpt = releaseRepository.findById(releaseId);
        if (releaseOpt.isEmpty()) {
            throw new IllegalArgumentException("Release not found with ID: " + releaseId);
        }

        releaseDeploymentRepository.deleteByReleaseIdAndDeploymentId(releaseId, deploymentId);
        return mapToDTO(releaseOpt.get());
    }

    public ReleaseDTO updateReleaseStatus(Long releaseId, String newStatus) {
        Optional<Release> releaseOpt = releaseRepository.findById(releaseId);
        if (releaseOpt.isEmpty()) {
            throw new IllegalArgumentException("Release not found with ID: " + releaseId);
        }

        Release release = releaseOpt.get();
        try {
            release.setStatus(Release.ReleaseStatus.valueOf(newStatus.toUpperCase()));
            if (release.getStatus() == Release.ReleaseStatus.DEPLOYED) {
                release.setActualDate(LocalDate.now());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        Release updated = releaseRepository.save(release);
        return mapToDTO(updated);
    }

    public void deleteRelease(Long releaseId) {
        Optional<Release> releaseOpt = releaseRepository.findById(releaseId);
        if (releaseOpt.isEmpty()) {
            throw new IllegalArgumentException("Release not found with ID: " + releaseId);
        }

        // Delete associated deployments first
        releaseDeploymentRepository.deleteByReleaseId(releaseId);
        
        // Delete the release
        releaseRepository.deleteById(releaseId);
    }

    public ReleaseSummaryDTO getReleaseSummary(Long releaseId) {
        Optional<Release> releaseOpt = releaseRepository.findById(releaseId);
        if (releaseOpt.isEmpty()) {
            throw new IllegalArgumentException("Release not found with ID: " + releaseId);
        }

        Release release = releaseOpt.get();
        List<ReleaseDeployment> deployments = releaseDeploymentRepository.findByReleaseId(releaseId);

        long totalDeployments = deployments.size();
        long totalChanges = deployments.stream()
                .mapToLong(rd -> metadataChangeService.getChangeCountByDeploymentId(rd.getDeploymentId()))
                .sum();

        return ReleaseSummaryDTO.builder()
                .releaseId(releaseId)
                .releaseName(release.getReleaseName())
                .version(release.getVersion())
                .status(release.getStatus().name())
                .totalDeployments(totalDeployments)
                .totalMetadataChanges(totalChanges)
                .plannedDate(release.getPlannedDate())
                .actualDate(release.getActualDate())
                .createdBy(release.getCreatedBy())
                .createdAt(release.getCreatedAt())
                .build();
    }

    private ReleaseDTO mapToDTO(Release release) {
        return ReleaseDTO.builder()
                .id(release.getId())
                .releaseName(release.getReleaseName())
                .version(release.getVersion())
                .status(release.getStatus().name())
                .plannedDate(release.getPlannedDate())
                .actualDate(release.getActualDate())
                .createdBy(release.getCreatedBy())
                .createdAt(release.getCreatedAt())
                .build();
    }

    // Inner DTO class for release summary
    public static class ReleaseSummaryDTO {
        public Long releaseId;
        public String releaseName;
        public String version;
        public String status;
        public Long totalDeployments;
        public Long totalMetadataChanges;
        public LocalDate plannedDate;
        public LocalDate actualDate;
        public String createdBy;
        public LocalDateTime createdAt;

        public static class ReleaseSummaryDTOBuilder {
            private Long releaseId;
            private String releaseName;
            private String version;
            private String status;
            private Long totalDeployments;
            private Long totalMetadataChanges;
            private LocalDate plannedDate;
            private LocalDate actualDate;
            private String createdBy;
            private LocalDateTime createdAt;

            public ReleaseSummaryDTOBuilder releaseId(Long releaseId) { this.releaseId = releaseId; return this; }
            public ReleaseSummaryDTOBuilder releaseName(String releaseName) { this.releaseName = releaseName; return this; }
            public ReleaseSummaryDTOBuilder version(String version) { this.version = version; return this; }
            public ReleaseSummaryDTOBuilder status(String status) { this.status = status; return this; }
            public ReleaseSummaryDTOBuilder totalDeployments(Long totalDeployments) { this.totalDeployments = totalDeployments; return this; }
            public ReleaseSummaryDTOBuilder totalMetadataChanges(Long totalMetadataChanges) { this.totalMetadataChanges = totalMetadataChanges; return this; }
            public ReleaseSummaryDTOBuilder plannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; return this; }
            public ReleaseSummaryDTOBuilder actualDate(LocalDate actualDate) { this.actualDate = actualDate; return this; }
            public ReleaseSummaryDTOBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
            public ReleaseSummaryDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

            public ReleaseSummaryDTO build() {
                ReleaseSummaryDTO dto = new ReleaseSummaryDTO();
                dto.releaseId = this.releaseId;
                dto.releaseName = this.releaseName;
                dto.version = this.version;
                dto.status = this.status;
                dto.totalDeployments = this.totalDeployments;
                dto.totalMetadataChanges = this.totalMetadataChanges;
                dto.plannedDate = this.plannedDate;
                dto.actualDate = this.actualDate;
                dto.createdBy = this.createdBy;
                dto.createdAt = this.createdAt;
                return dto;
            }
        }

        public static ReleaseSummaryDTOBuilder builder() {
            return new ReleaseSummaryDTOBuilder();
        }
    }
}
