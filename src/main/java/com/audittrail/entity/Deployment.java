package com.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private String deployedBy;

    @Column(name = "deployment_time", nullable = false)
    private LocalDateTime deploymentTime;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (deploymentTime == null) {
            deploymentTime = LocalDateTime.now();
        }
    }

    public enum DeploymentStatus {
        PLANNED, IN_PROGRESS, DEPLOYED, ROLLED_BACK, FAILED
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
