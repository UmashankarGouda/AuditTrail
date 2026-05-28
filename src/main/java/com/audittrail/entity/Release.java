package com.audittrail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "releases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String releaseName;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseStatus status;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReleaseStatus.PLANNED;
        }
    }

    public enum ReleaseStatus {
        PLANNED, IN_PROGRESS, DEPLOYED, ROLLED_BACK
    }
}
