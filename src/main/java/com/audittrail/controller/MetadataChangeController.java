package com.audittrail.controller;

import com.audittrail.dto.MetadataChangeDTO;
import com.audittrail.service.MetadataChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
@Tag(name = "Metadata Changes", description = "Metadata change tracking and audit endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class MetadataChangeController {

    @Autowired
    private MetadataChangeService metadataChangeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get all metadata changes", description = "Retrieve all metadata changes across all deployments")
    public ResponseEntity<Page<MetadataChangeDTO>> getAllMetadataChanges(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MetadataChangeDTO> changes = metadataChangeService.getAllMetadataChangesPaged(pageable);
        return ResponseEntity.ok(changes);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Log a metadata change", description = "Record a single metadata change")
    public ResponseEntity<MetadataChangeDTO> logMetadataChange(@RequestBody Map<String, String> request) {
        // Accept "deployment" or "deploymentId"
        String deploymentStr = request.get("deployment") != null ? request.get("deployment") : request.get("deploymentId");
        Long deploymentId = Long.parseLong(deploymentStr);
        
        // Accept "description" or "componentName" - default to empty if not provided
        String componentName = request.getOrDefault("description", request.getOrDefault("componentName", "Metadata Change"));
        String componentType = request.getOrDefault("componentType", "GENERAL");
        String changeType = request.get("changeType");
        String oldValue = request.getOrDefault("oldValue", "");
        String newValue = request.getOrDefault("newValue", "");
        String changedBy = request.getOrDefault("changedBy", "system");

        MetadataChangeDTO change = metadataChangeService.logMetadataChange(
                deploymentId, componentName, componentType, changeType, oldValue, newValue, changedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(change);
    }

    @GetMapping("/deployment/{deploymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get all changes for a deployment", description = "Retrieve all metadata changes associated with a specific deployment")
    public ResponseEntity<Page<MetadataChangeDTO>> getChangesByDeploymentId(
            @Parameter(description = "Deployment ID") @PathVariable Long deploymentId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MetadataChangeDTO> changes = metadataChangeService.getChangesByDeploymentIdPaged(deploymentId, pageable);
        return ResponseEntity.ok(changes);
    }

    @GetMapping("/component/{componentName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get full history of a component", description = "Retrieve all changes for a specific component across all deployments")
    public ResponseEntity<Page<MetadataChangeDTO>> getComponentHistory(
            @Parameter(description = "Component name") @PathVariable String componentName,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MetadataChangeDTO> history = metadataChangeService.getComponentHistoryPaged(componentName, pageable);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete all metadata changes", description = "WARNING: This will delete ALL metadata change records from the database")
    public ResponseEntity<Map<String, Object>> deleteAllMetadataChanges() {
        long deletedCount = metadataChangeService.deleteAllMetadataChanges();
        return ResponseEntity.ok(Map.of(
            "message", "All metadata changes deleted successfully",
            "deletedRecords", deletedCount
        ));
    }
}
