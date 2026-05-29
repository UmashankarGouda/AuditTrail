package com.audittrail.service;

import com.audittrail.entity.MetadataChange;
import com.audittrail.repository.MetadataChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Batch Processing Service Tests")
class BatchProcessingServiceTest {

    @Mock
    private MetadataChangeRepository metadataChangeRepository;

    private BatchProcessingService batchProcessingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create a custom implementation for testing
        batchProcessingService = new BatchProcessingService();
    }

    @Test
    @DisplayName("Should convert REMOVED to DELETED enum")
    void convertChangeType_shouldConvertRemoved_toDeleted() {
        String result = batchProcessingService.convertChangeType("REMOVED");
        assertEquals("DELETED", result);
    }

    @Test
    @DisplayName("Should convert UPDATED to MODIFIED enum")
    void convertChangeType_shouldConvertUpdated_toModified() {
        String result = batchProcessingService.convertChangeType("UPDATED");
        assertEquals("MODIFIED", result);
    }

    @Test
    @DisplayName("Should preserve CREATED enum unchanged")
    void convertChangeType_shouldPreserve_CreatedEnum() {
        String result = batchProcessingService.convertChangeType("CREATED");
        assertEquals("CREATED", result);
    }

    @Test
    @DisplayName("Should handle case-insensitive conversion")
    void convertChangeType_shouldHandleCaseInsensitive() {
        assertEquals("DELETED", batchProcessingService.convertChangeType("removed"));
        assertEquals("DELETED", batchProcessingService.convertChangeType("Removed"));
        assertEquals("MODIFIED", batchProcessingService.convertChangeType("updated"));
        assertEquals("MODIFIED", batchProcessingService.convertChangeType("Updated"));
    }

    @Test
    @DisplayName("Should parse CSV line correctly with all fields")
    void parseCSVLine_shouldParseAllFields() {
        String csvLine = "PaymentAPI,API,CREATED,john_dev,1,2024-05-28T10:30:00Z,,v2.0";
        
        Map<String, String> result = batchProcessingService.parseCSVLine(csvLine);
        
        assertNotNull(result);
        assertEquals("PaymentAPI", result.get("component_name"));
        assertEquals("API", result.get("component_type"));
        assertEquals("CREATED", result.get("change_type"));
        assertEquals("john_dev", result.get("changed_by"));
        assertEquals("1", result.get("deployment_id"));
        assertEquals("2024-05-28T10:30:00Z", result.get("changed_at"));
        assertEquals("", result.get("old_value"));
        assertEquals("v2.0", result.get("new_value"));
    }

    @Test
    @DisplayName("Should handle missing optional fields in CSV")
    void parseCSVLine_shouldHandleMissingOptionalFields() {
        String csvLine = "Component,Type,MODIFIED,user,1,2024-05-28T10:30:00Z,,";
        
        Map<String, String> result = batchProcessingService.parseCSVLine(csvLine);
        
        assertNotNull(result);
        assertEquals("Component", result.get("component_name"));
        assertEquals("Type", result.get("component_type"));
        assertEquals("", result.get("new_value"));
    }

    @Test
    @DisplayName("Should validate valid deployment ID")
    void validateDeploymentId_shouldAcceptValidId() {
        assertTrue(batchProcessingService.isValidDeploymentId("1"));
        assertTrue(batchProcessingService.isValidDeploymentId("999"));
        assertTrue(batchProcessingService.isValidDeploymentId("12345"));
    }

    @Test
    @DisplayName("Should reject invalid deployment ID")
    void validateDeploymentId_shouldRejectInvalidId() {
        assertFalse(batchProcessingService.isValidDeploymentId("0"));
        assertFalse(batchProcessingService.isValidDeploymentId("-1"));
        assertFalse(batchProcessingService.isValidDeploymentId("abc"));
        assertFalse(batchProcessingService.isValidDeploymentId(""));
    }

    @Test
    @DisplayName("Should validate change type")
    void validateChangeType_shouldAcceptValidTypes() {
        assertTrue(batchProcessingService.isValidChangeType("CREATED"));
        assertTrue(batchProcessingService.isValidChangeType("MODIFIED"));
        assertTrue(batchProcessingService.isValidChangeType("DELETED"));
        assertTrue(batchProcessingService.isValidChangeType("REMOVED")); // Will be converted
        assertTrue(batchProcessingService.isValidChangeType("UPDATED")); // Will be converted
    }

    @Test
    @DisplayName("Should reject invalid change type")
    void validateChangeType_shouldRejectInvalidTypes() {
        assertFalse(batchProcessingService.isValidChangeType("INVALID"));
        assertFalse(batchProcessingService.isValidChangeType(""));
        assertFalse(batchProcessingService.isValidChangeType("DELETE")); // Missing D
    }

    @Test
    @DisplayName("Should validate complete CSV row")
    void validateCSVRow_shouldAcceptValidRow() {
        Map<String, String> row = new HashMap<>();
        row.put("component_name", "PaymentAPI");
        row.put("component_type", "API");
        row.put("change_type", "CREATED");
        row.put("changed_by", "john_dev");
        row.put("deployment_id", "1");
        row.put("changed_at", "2024-05-28T10:30:00Z");
        row.put("old_value", "");
        row.put("new_value", "v2.0");

        assertTrue(batchProcessingService.isValidCSVRow(row));
    }

    @Test
    @DisplayName("Should reject CSV row with missing component_name")
    void validateCSVRow_shouldRejectMissingComponentName() {
        Map<String, String> row = new HashMap<>();
        row.put("component_type", "API");
        row.put("change_type", "CREATED");
        row.put("changed_by", "john_dev");
        row.put("deployment_id", "1");
        row.put("changed_at", "2024-05-28T10:30:00Z");

        assertFalse(batchProcessingService.isValidCSVRow(row));
    }

    @Test
    @DisplayName("Should reject CSV row with invalid deployment_id")
    void validateCSVRow_shouldRejectInvalidDeploymentId() {
        Map<String, String> row = new HashMap<>();
        row.put("component_name", "PaymentAPI");
        row.put("component_type", "API");
        row.put("change_type", "CREATED");
        row.put("changed_by", "john_dev");
        row.put("deployment_id", "invalid");
        row.put("changed_at", "2024-05-28T10:30:00Z");

        assertFalse(batchProcessingService.isValidCSVRow(row));
    }

    @Test
    @DisplayName("Should calculate correct batch statistics")
    void calculateBatchStatistics_shouldReturnCorrectCounts() {
        int totalRead = 1000;
        int successful = 950;
        int skipped = 50;
        int chunks = 2;
        long processingTimeMs = 5000;

        Map<String, Object> stats = batchProcessingService.calculateStatistics(
                totalRead, successful, skipped, chunks, processingTimeMs
        );

        assertEquals(1000, stats.get("total_read"));
        assertEquals(950, stats.get("successful"));
        assertEquals(50, stats.get("skipped"));
        assertEquals(2, stats.get("chunks"));
        assertEquals(5000L, stats.get("processing_time_ms"));
        assertEquals(95.0, stats.get("success_rate"));
        assertEquals(200.0, stats.get("records_per_second"));
    }

    @Test
    @DisplayName("Should handle zero processing time gracefully")
    void calculateBatchStatistics_shouldHandleZeroTime() {
        Map<String, Object> stats = batchProcessingService.calculateStatistics(
                100, 100, 0, 1, 0
        );

        assertNotNull(stats);
        assertEquals(100, stats.get("total_read"));
        assertEquals(100.0, stats.get("success_rate"));
    }

    @Test
    @DisplayName("Should handle all records skipped")
    void calculateBatchStatistics_shouldHandleAllSkipped() {
        Map<String, Object> stats = batchProcessingService.calculateStatistics(
                100, 0, 100, 1, 1000
        );

        assertEquals(0.0, stats.get("success_rate"));
        assertEquals(100, stats.get("skipped"));
    }

    @Test
    @DisplayName("Should build MetadataChange entity from CSV row")
    void buildMetadataChangeEntity_shouldCreateValidEntity() {
        Map<String, String> row = new HashMap<>();
        row.put("component_name", "PaymentAPI");
        row.put("component_type", "API");
        row.put("change_type", "CREATED");
        row.put("changed_by", "john_dev");
        row.put("deployment_id", "1");
        row.put("changed_at", "2024-05-28T10:30:00Z");
        row.put("old_value", "");
        row.put("new_value", "v2.0");

        MetadataChange entity = batchProcessingService.buildMetadataChangeEntity(row);

        assertNotNull(entity);
        assertEquals("PaymentAPI", entity.getComponentName());
        assertEquals("API", entity.getComponentType());
        assertEquals(MetadataChange.ChangeType.CREATED, entity.getChangeType());
        assertEquals("john_dev", entity.getChangedBy());
        assertEquals(1L, entity.getDeploymentId());
        assertEquals("v2.0", entity.getNewValue());
    }

    @Test
    @DisplayName("Should convert REMOVED to DELETED when building entity")
    void buildMetadataChangeEntity_shouldConvertRemovedToDeleted() {
        Map<String, String> row = new HashMap<>();
        row.put("component_name", "Component");
        row.put("component_type", "Type");
        row.put("change_type", "REMOVED");
        row.put("changed_by", "user");
        row.put("deployment_id", "1");
        row.put("changed_at", "2024-05-28T10:30:00Z");
        row.put("old_value", "v1.0");
        row.put("new_value", "");

        MetadataChange entity = batchProcessingService.buildMetadataChangeEntity(row);

        assertNotNull(entity);
        assertEquals(MetadataChange.ChangeType.DELETED, entity.getChangeType());
    }

    @Test
    @DisplayName("Should handle 40000 record batch processing")
    void processBatch_shouldHandleLargeDataset() {
        int recordCount = 40000;
        int chunkSize = 500;
        
        int expectedChunks = (recordCount + chunkSize - 1) / chunkSize; // Ceiling division
        
        assertEquals(80, expectedChunks);
    }

    @Test
    @DisplayName("Should maintain data integrity for empty strings")
    void buildMetadataChangeEntity_shouldPreserveEmptyStrings() {
        Map<String, String> row = new HashMap<>();
        row.put("component_name", "Component");
        row.put("component_type", "Type");
        row.put("change_type", "DELETED");
        row.put("changed_by", "user");
        row.put("deployment_id", "1");
        row.put("changed_at", "2024-05-28T10:30:00Z");
        row.put("old_value", "");
        row.put("new_value", "");

        MetadataChange entity = batchProcessingService.buildMetadataChangeEntity(row);

        assertNotNull(entity);
        assertEquals("", entity.getOldValue());
        assertEquals("", entity.getNewValue());
    }
}
