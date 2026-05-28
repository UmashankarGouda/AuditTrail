package com.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metadata_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private Long deploymentId;

    @Column(nullable = false)
    private String componentName;

    @Column(nullable = false)
    private String componentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public enum ChangeType {
        CREATED, MODIFIED, DELETED
    }
}
