package com.audittrail.service;

import com.audittrail.dto.MetadataChangeDTO;
import com.audittrail.entity.MetadataChange;
import com.audittrail.repository.MetadataChangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MetadataChangeService {

    @Autowired
    private MetadataChangeRepository metadataChangeRepository;

    public MetadataChangeDTO logMetadataChange(Long deploymentId, String componentName, 
                                               String componentType, String changeType, 
                                               String oldValue, String newValue, String changedBy) {
        MetadataChange.ChangeType type;
        try {
            type = MetadataChange.ChangeType.valueOf(changeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = MetadataChange.ChangeType.MODIFIED;
        }

        MetadataChange change = MetadataChange.builder()
                .deploymentId(deploymentId)
                .componentName(componentName)
                .componentType(componentType)
                .changeType(type)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();

        MetadataChange saved = metadataChangeRepository.save(change);
        return mapToDTO(saved);
    }

    public List<MetadataChangeDTO> getChangesByDeploymentId(Long deploymentId) {
        return metadataChangeRepository.findByDeploymentId(deploymentId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<MetadataChangeDTO> getAllMetadataChangesPaged(Pageable pageable) {
        return metadataChangeRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    public Page<MetadataChangeDTO> getChangesByDeploymentIdPaged(Long deploymentId, Pageable pageable) {
        return metadataChangeRepository.findByDeploymentId(deploymentId, pageable)
                .map(this::mapToDTO);
    }

    public List<MetadataChangeDTO> getComponentHistory(String componentName) {
        return metadataChangeRepository.findByComponentName(componentName)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Page<MetadataChangeDTO> getComponentHistoryPaged(String componentName, Pageable pageable) {
        return metadataChangeRepository.findByComponentName(componentName, pageable)
                .map(this::mapToDTO);
    }

    public Optional<MetadataChangeDTO> getChangeById(Long id) {
        return metadataChangeRepository.findById(id).map(this::mapToDTO);
    }

    public long getChangeCountByDeploymentId(Long deploymentId) {
        return metadataChangeRepository.countByDeploymentId(deploymentId);
    }

    public long deleteAllMetadataChanges() {
        long count = metadataChangeRepository.count();
        metadataChangeRepository.deleteAll();
        return count;
    }

    private MetadataChangeDTO mapToDTO(MetadataChange change) {
        return MetadataChangeDTO.builder()
                .id(change.getId())
                .deploymentId(change.getDeploymentId())
                .componentName(change.getComponentName())
                .componentType(change.getComponentType())
                .changeType(change.getChangeType().name())
                .oldValue(change.getOldValue())
                .newValue(change.getNewValue())
                .changedBy(change.getChangedBy())
                .changedAt(change.getChangedAt())
                .build();
    }
}
