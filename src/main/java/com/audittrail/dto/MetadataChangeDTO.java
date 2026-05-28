package com.audittrail.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataChangeDTO {
    private Long id;
    private Long deploymentId;
    private String componentName;
    private String componentType;
    private String changeType;
    private String oldValue;
    private String newValue;
    private String changedBy;
    private LocalDateTime changedAt;
}
