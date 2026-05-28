package com.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "release_deployments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseDeployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;
}
