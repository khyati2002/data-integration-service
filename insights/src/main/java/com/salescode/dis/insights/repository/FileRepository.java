package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, String> {
    Page<FileEntity> findByJobId(String jobId, Pageable pageable);

    Optional<FileEntity> findByFileIdAndMaster(String fileId, String master);

    boolean existsByFileIdAndMaster(String fileId, String master);

    @Query("SELECT f FROM FileEntity f " +
            "WHERE (f.status = :status) " +
            "AND f.lastModifiedTime < :staleCutoffTime " +
            "AND f.lastModifiedTime >= :tooOldCutoffTime")
    List<FileEntity> findAllStalePendingFiles(
            @Param("staleCutoffTime") Instant staleCutoffTime,
            @Param("tooOldCutoffTime") Instant tooOldCutoffTime,
            @Param("status")ProgressStatus status
            );
}
