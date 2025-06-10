package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.salescode.dis.insights.enums.IntegrationMode;

public interface FileRepository extends JpaRepository<FileEntity, String> {
    Page<FileEntity> findByJobId(String jobId, Pageable pageable);

    @Query("SELECT f FROM FileEntity f " +
            "WHERE  f.modeOfIntegration = com.salescode.dis.insights.enums.IntegrationMode.API OR f.modeOfIntegration = com.salescode.dis.insights.enums.IntegrationMode.MDM " +
            "  AND (f.consumedStatus = :status OR f.publishedStatus = :status) " +
            "  AND f.lastModifiedTime < :staleCutoffTime " +
            "  AND f.lastModifiedTime >= :tooOldCutoffTime")
    List<FileEntity> findStaleApiBasedPendingFiles(
            @Param("staleCutoffTime") Instant staleCutoffTime,
            @Param("tooOldCutoffTime") Instant tooOldCutoffTime,
            @Param("status") FileStatus status
    );

    @Query("SELECT f FROM FileEntity f " +
            "WHERE (f.consumedStatus = :status OR f.publishedStatus = :status) " +
            "AND f.lastModifiedTime < :staleCutoffTime " +
            "AND f.lastModifiedTime >= :tooOldCutoffTime")
    List<FileEntity> findAllStalePendingFiles(
            @Param("staleCutoffTime") Instant staleCutoffTime,
            @Param("tooOldCutoffTime") Instant tooOldCutoffTime,
            @Param("status") FileStatus status
    );

    Optional<FileEntity> findByFileIdAndMaster(String fileId, String master);

    boolean existsByFileIdAndMaster(String fileId, String master);
}
