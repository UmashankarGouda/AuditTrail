package com.audittrail.batch;

import com.audittrail.entity.MetadataChange;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MetadataChangeItemProcessor implements ItemProcessor<MetadataChangeCsvRecord, MetadataChange> {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public MetadataChange process(MetadataChangeCsvRecord record) throws Exception {
        // Validate required fields
        if (record.getComponentName() == null || record.getComponentName().isEmpty() ||
            record.getDeploymentId() == null || record.getChangedBy() == null) {
            throw new IllegalArgumentException("Missing required fields in CSV record");
        }

        MetadataChange.ChangeType changeType;
        try {
            changeType = MetadataChange.ChangeType.valueOf(record.getChangeType().toUpperCase());
        } catch (IllegalArgumentException e) {
            changeType = MetadataChange.ChangeType.MODIFIED;
        }

        LocalDateTime changedAt;
        try {
            if (record.getChangedAt() != null && !record.getChangedAt().isEmpty()) {
                changedAt = LocalDateTime.parse(record.getChangedAt(), dateTimeFormatter);
            } else {
                changedAt = LocalDateTime.now();
            }
        } catch (Exception e) {
            changedAt = LocalDateTime.now();
        }

        return MetadataChange.builder()
                .deploymentId(record.getDeploymentId())
                .componentName(record.getComponentName())
                .componentType(record.getComponentType() != null ? record.getComponentType() : "UNKNOWN")
                .changeType(changeType)
                .oldValue(record.getOldValue())
                .newValue(record.getNewValue())
                .changedBy(record.getChangedBy())
                .changedAt(changedAt)
                .build();
    }
}
