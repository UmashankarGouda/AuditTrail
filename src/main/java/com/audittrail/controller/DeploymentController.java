package com.audittrail.controller;

import com.audittrail.dto.DeploymentDTO;
import com.audittrail.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/deployments")
@Tag(name = "Deployments", description = "Deployment management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DeploymentController {

    @Autowired
    private DeploymentService deploymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Create a new deployment", description = "Create a deployment record with automatic risk assessment")
    @ApiResponse(responseCode = "201", description = "Deployment created successfully")
    public ResponseEntity<DeploymentDTO> createDeployment(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String environment = request.get("environment");
        String deployedBy = request.get("deployedBy");
        String notes = request.get("notes");

        DeploymentDTO deployment = deploymentService.createDeployment(name, environment, deployedBy, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(deployment);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get all deployments (paginated)", description = "Retrieve paginated list of deployments")
    @ApiResponse(responseCode = "200", description = "Deployments retrieved successfully")
    public ResponseEntity<Page<DeploymentDTO>> getAllDeployments(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DeploymentDTO> deployments = deploymentService.getAllDeployments(pageable);
        return ResponseEntity.ok(deployments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Get deployment by ID", description = "Retrieve a specific deployment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deployment found"),
        @ApiResponse(responseCode = "404", description = "Deployment not found")
    })
    public ResponseEntity<DeploymentDTO> getDeploymentById(
            @Parameter(description = "Deployment ID") @PathVariable Long id) {
        Optional<DeploymentDTO> deployment = deploymentService.getDeploymentById(id);
        return deployment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/environment/{env}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER', 'VIEWER')")
    @Operation(summary = "Filter deployments by environment", description = "Get all deployments for a specific environment")
    @ApiResponse(responseCode = "200", description = "Deployments filtered successfully")
    public ResponseEntity<List<DeploymentDTO>> getDeploymentsByEnvironment(
            @Parameter(description = "Environment name") @PathVariable String env) {
        List<DeploymentDTO> deployments = deploymentService.getDeploymentsByEnvironment(env);
        return ResponseEntity.ok(deployments);
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Get all HIGH_RISK deployments", description = "Retrieve deployments marked as high-risk")
    @ApiResponse(responseCode = "200", description = "High-risk deployments retrieved")
    public ResponseEntity<List<DeploymentDTO>> getHighRiskDeployments() {
        List<DeploymentDTO> deployments = deploymentService.getAllHighRiskDeployments();
        return ResponseEntity.ok(deployments);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Update deployment status", description = "Change deployment status (PLANNED, IN_PROGRESS, DEPLOYED, etc.)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "404", description = "Deployment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<DeploymentDTO> updateDeploymentStatus(
            @Parameter(description = "Deployment ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            DeploymentDTO updated = deploymentService.updateDeploymentStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a deployment", description = "Remove deployment record (ADMIN only)")
    @ApiResponse(responseCode = "204", description = "Deployment deleted successfully")
    public ResponseEntity<Void> deleteDeployment(
            @Parameter(description = "Deployment ID") @PathVariable Long id) {
        deploymentService.deleteDeployment(id);
        return ResponseEntity.noContent().build();
    }
}
