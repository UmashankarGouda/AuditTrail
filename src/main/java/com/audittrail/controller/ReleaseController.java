package com.audittrail.controller;

import com.audittrail.dto.ReleaseDTO;
import com.audittrail.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/releases")
@Tag(name = "Releases", description = "Release management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class ReleaseController {

    @Autowired
    private ReleaseService releaseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Create a new release", description = "Create a release record")
    public ResponseEntity<ReleaseDTO> createRelease(@RequestBody Map<String, String> request) {
        String version = request.get("version");
        String releaseName = request.get("description"); // Accept "description" as release name
        if (releaseName == null || releaseName.isEmpty()) {
            releaseName = "Release " + version;
        }
        String releaseDateStr = request.get("releaseDate");
        String createdBy = request.get("createdBy");
        
        // Auto-set createdBy to "system" if not provided
        if (createdBy == null || createdBy.isEmpty()) {
            createdBy = "system";
        }

        LocalDate releaseDate = null;
        if (releaseDateStr != null && !releaseDateStr.isEmpty()) {
            releaseDate = LocalDate.parse(releaseDateStr);
        }

        ReleaseDTO release = releaseService.createRelease(releaseName, version, releaseDate, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(release);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get all releases (paginated)", description = "Retrieve paginated list of releases")
    public ResponseEntity<Page<ReleaseDTO>> getAllReleases(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReleaseDTO> releases = releaseService.getAllReleases(pageable);
        return ResponseEntity.ok(releases);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get release by ID", description = "Retrieve a specific release")
    public ResponseEntity<ReleaseDTO> getReleaseById(
            @Parameter(description = "Release ID") @PathVariable Long id) {
        return releaseService.getReleaseById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{releaseId}/deployments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Assign deployment to a release", description = "Add a deployment to a release")
    public ResponseEntity<?> assignDeploymentToRelease(
            @Parameter(description = "Release ID") @PathVariable Long releaseId,
            @RequestBody Map<String, Long> request) {
        try {
            Long deploymentId = request.get("deploymentId");
            ReleaseDTO release = releaseService.assignDeploymentToRelease(releaseId, deploymentId);
            return ResponseEntity.ok(release);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get full release summary", description = "Retrieve release summary with all aggregated changes")
    public ResponseEntity<?> getReleaseSummary(
            @Parameter(description = "Release ID") @PathVariable Long id) {
        try {
            ReleaseService.ReleaseSummaryDTO summary = releaseService.getReleaseSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Update release status", description = "Change release status")
    public ResponseEntity<?> updateReleaseStatus(
            @Parameter(description = "Release ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            ReleaseDTO updated = releaseService.updateReleaseStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
