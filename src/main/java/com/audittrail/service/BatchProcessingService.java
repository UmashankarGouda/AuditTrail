package com.audittrail.service;

import com.audittrail.entity.MetadataChange;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling batch CSV processing operations.
 * Includes CSV parsing, validation, and enum conversion.
 */
@Service
public class BatchProcessingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final int CSV_FIELD_COUNT = 8;

    /**
     * Convert CSV enum values to system enum values.
     * REMOVED -> DELETED
     * UPDATED -> MODIFIED
     * 
     * @param changeTypeStr Original change type from CSV
     * @return Converted change type
     */
    public String convertChangeType(String changeTypeStr) {
        if (changeTypeStr == null) {
            return "MODIFIED";
        }

        String upper = changeTypeStr.toUpperCase().trim();
        if ("REMOVED".equals(upper)) {
            return "DELETED";
        } else if ("UPDATED".equals(upper)) {
            return "MODIFIED";
        }
        return upper;
    }

    /**
     * Parse a CSV line into a map of field name -> value.
     * Expected order: component_name, component_type, change_type, changed_by, 
     *                 deployment_id, changed_at, old_value, new_value
     * 
     * @param csvLine CSV line to parse
     * @return Map of field names to values
     */
    public Map<String, String> parseCSVLine(String csvLine) {
        Map<String, String> result = new HashMap<>();
        
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return result;
        }

        // Simple CSV parsing - handles basic case (no quoted values with commas)
        String[] fields = csvLine.split(",", -1); // -1 preserves trailing empty strings

        if (fields.length >= CSV_FIELD_COUNT) {
            result.put("component_name", fields[0].trim());
            result.put("component_type", fields[1].trim());
            result.put("change_type", fields[2].trim());
            result.put("changed_by", fields[3].trim());
            result.put("deployment_id", fields[4].trim());
            result.put("changed_at", fields[5].trim());
            result.put("old_value", fields[6].trim());
            result.put("new_value", fields[7].trim());
        }

        return result;
    }

    /**
     * Validate if deployment ID is valid.
     * 
     * @param deploymentIdStr Deployment ID as string
     * @return true if valid positive integer
     */
    public boolean isValidDeploymentId(String deploymentIdStr) {
        try {
            long id = Long.parseLong(deploymentIdStr);
            return id > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate if change type is supported.
     * 
     * @param changeType Change type to validate
     * @return true if CREATED, MODIFIED, DELETED, REMOVED, or UPDATED
     */
    public boolean isValidChangeType(String changeType) {
        if (changeType == null || changeType.trim().isEmpty()) {
            return false;
        }

        String upper = changeType.toUpperCase().trim();
        return upper.equals("CREATED") || upper.equals("MODIFIED") || upper.equals("DELETED") ||
               upper.equals("REMOVED") || upper.equals("UPDATED");
    }

    /**
     * Validate a complete CSV row.
     * 
     * @param row Map of CSV fields
     * @return true if row passes validation
     */
    public boolean isValidCSVRow(Map<String, String> row) {
        // Check required fields exist
        if (!row.containsKey("component_name") || row.get("component_name").isEmpty()) {
            return false;
        }
        if (!row.containsKey("component_type") || row.get("component_type").isEmpty()) {
            return false;
        }
        if (!row.containsKey("change_type") || !isValidChangeType(row.get("change_type"))) {
            return false;
        }
        if (!row.containsKey("changed_by") || row.get("changed_by").isEmpty()) {
            return false;
        }
        if (!row.containsKey("deployment_id") || !isValidDeploymentId(row.get("deployment_id"))) {
            return false;
        }
        if (!row.containsKey("changed_at") || row.get("changed_at").isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Calculate batch processing statistics.
     * 
     * @param totalRead Total records read
     * @param successful Records successfully processed
     * @param skipped Records skipped due to errors
     * @param chunks Number of chunks processed
     * @param processingTimeMs Time taken in milliseconds
     * @return Map with statistics
     */
    public Map<String, Object> calculateStatistics(int totalRead, int successful, int skipped, 
                                                    int chunks, long processingTimeMs) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_read", totalRead);
        stats.put("successful", successful);
        stats.put("skipped", skipped);
        stats.put("chunks", chunks);
        stats.put("processing_time_ms", processingTimeMs);

        // Calculate success rate
        double successRate = totalRead > 0 ? (double) successful / totalRead * 100 : 0;
        stats.put("success_rate", successRate);

        // Calculate throughput (records per second)
        double recordsPerSecond = processingTimeMs > 0 
            ? (double) totalRead / processingTimeMs * 1000 
            : 0;
        stats.put("records_per_second", recordsPerSecond);

        return stats;
    }

    /**
     * Build a MetadataChange entity from a parsed CSV row.
     * Applies enum conversion (REMOVED->DELETED, UPDATED->MODIFIED).
     * 
     * @param row Parsed CSV row
     * @return MetadataChange entity
     */
    public MetadataChange buildMetadataChangeEntity(Map<String, String> row) {
        // Convert change type
        String changeTypeStr = convertChangeType(row.get("change_type"));
        MetadataChange.ChangeType changeType;
        try {
            changeType = MetadataChange.ChangeType.valueOf(changeTypeStr);
        } catch (IllegalArgumentException e) {
            changeType = MetadataChange.ChangeType.MODIFIED; // Default fallback
        }

        // Parse timestamp
        LocalDateTime changedAt;
        try {
            changedAt = LocalDateTime.parse(row.get("changed_at"), DATE_FORMATTER);
        } catch (Exception e) {
            changedAt = LocalDateTime.now();
        }

        return MetadataChange.builder()
                .deploymentId(Long.parseLong(row.get("deployment_id")))
                .componentName(row.get("component_name"))
                .componentType(row.get("component_type"))
                .changeType(changeType)
                .oldValue(row.get("old_value"))
                .newValue(row.get("new_value"))
                .changedBy(row.get("changed_by"))
                .changedAt(changedAt)
                .build();
    }

    /**
     * Get CSV header fields in correct order.
     * 
     * @return Array of header field names
     */
    public String[] getCSVHeader() {
        return new String[]{
            "component_name", "component_type", "change_type", "changed_by",
            "deployment_id", "changed_at", "old_value", "new_value"
        };
    }

    /**
     * Validate CSV header format.
     * 
     * @param headerLine CSV header line
     * @return true if header matches expected format
     */
    public boolean isValidCSVHeader(String headerLine) {
        if (headerLine == null || headerLine.trim().isEmpty()) {
            return false;
        }

        String[] headers = headerLine.split(",", -1);
        String[] expectedHeaders = getCSVHeader();

        if (headers.length != expectedHeaders.length) {
            return false;
        }

        for (int i = 0; i < headers.length; i++) {
            if (!headers[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                return false;
            }
        }

        return true;
    }
}
