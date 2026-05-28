package com.audittrail.repository;

import com.audittrail.entity.Release;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {
    Page<Release> findAll(Pageable pageable);
    
    List<Release> findByStatus(Release.ReleaseStatus status);
    
    boolean existsByVersion(String version);
}
