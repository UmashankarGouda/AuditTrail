package com.audittrail.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseDTO {
    private Long id;
    private String releaseName;
    private String version;
    private String status;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private String createdBy;
    private LocalDateTime createdAt;
}
