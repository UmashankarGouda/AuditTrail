package com.audittrail.batch;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataChangeCsvRecord {
    private String componentName;
    private String componentType;
    private String changeType;
    private String changedBy;
    private Long deploymentId;
    private String changedAt;
    private String oldValue;
    private String newValue;
}
