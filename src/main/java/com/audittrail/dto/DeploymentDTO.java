package com.audittrail.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentDTO {
    private Long id;
    private String name;
    private String environment;
    private String status;
    private String riskLevel;
    private String deployedBy;
    private LocalDateTime deploymentTime;
    private String notes;
    private LocalDateTime createdAt;
}
