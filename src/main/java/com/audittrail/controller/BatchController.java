package com.audittrail.controller;

import com.audittrail.entity.MetadataChange;
import com.audittrail.repository.MetadataChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch Processing", description = "CSV batch processing for metadata changes")
@SecurityRequirement(name = "Bearer Authentication")
public class BatchController {

    @Autowired
    private MetadataChangeRepository metadataChangeRepository;

    @PostMapping(value = "/upload-metadata-csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Upload and process CSV", description = "Upload CSV file with metadata changes. Processes 500 records per chunk, tolerates bad records")
    public ResponseEntity<Map<String, Object>> uploadAndProcessCSV(@RequestPart("file") MultipartFile file) {
        try {
            long startTime = System.currentTimeMillis();
            int totalRecords = 0;
            int successCount = 0;
            int skipCount = 0;
            int chunkNumber = 0;
            List<MetadataChange> chunk = new ArrayList<>();
            int CHUNK_SIZE = 500;

            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            boolean headerSkipped = false;

            Map<String, Object> response = new HashMap<>();
            List<String> processedChunks = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                totalRecords++;

                try {
                    String[] fields = line.split(",");
                    if (fields.length < 8) {
                        skipCount++;
                        continue;
                    }

                    MetadataChange change = new MetadataChange();
                    change.setComponentName(fields[0].trim());
                    change.setComponentType(fields[1].trim());
                    
                    // Convert CSV changeType values to enum (REMOVED→DELETED, UPDATED→MODIFIED)
                    String changeTypeStr = fields[2].trim();
                    if ("REMOVED".equalsIgnoreCase(changeTypeStr)) {
                        changeTypeStr = "DELETED";
                    } else if ("UPDATED".equalsIgnoreCase(changeTypeStr)) {
                        changeTypeStr = "MODIFIED";
                    }
                    change.setChangeType(MetadataChange.ChangeType.valueOf(changeTypeStr));
                    
                    change.setChangedBy(fields[3].trim());
                    change.setDeploymentId(Long.parseLong(fields[4].trim()));
                    change.setChangedAt(LocalDateTime.now());
                    change.setOldValue(fields[6].trim());
                    change.setNewValue(fields[7].trim());

                    chunk.add(change);

                    if (chunk.size() >= CHUNK_SIZE) {
                        chunkNumber++;
                        metadataChangeRepository.saveAll(chunk);
                        successCount += chunk.size();
                        processedChunks.add("Chunk " + chunkNumber + ": " + chunk.size() + " records saved");
                        chunk.clear();
                    }
                } catch (Exception e) {
                    skipCount++;
                }
            }

            // Save remaining records
            if (!chunk.isEmpty()) {
                chunkNumber++;
                metadataChangeRepository.saveAll(chunk);
                successCount += chunk.size();
                processedChunks.add("Chunk " + chunkNumber + ": " + chunk.size() + " records saved");
            }

            long processingTime = System.currentTimeMillis() - startTime;

            response.put("message", "Batch processing completed successfully");
            response.put("filename", file.getOriginalFilename());
            response.put("totalRecordsRead", totalRecords);
            response.put("successfulRecords", successCount);
            response.put("skippedRecords", skipCount);
            response.put("chunksProcessed", chunkNumber);
            response.put("chunkSize", CHUNK_SIZE);
            response.put("processingTimeMs", processingTime);
            response.put("recordsPerSecond", totalRecords > 0 ? (totalRecords * 1000) / processingTime : 0);
            response.put("details", processedChunks);

            reader.close();
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process CSV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

