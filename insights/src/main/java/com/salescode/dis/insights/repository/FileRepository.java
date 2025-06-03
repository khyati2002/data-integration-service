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

public interface FileRepository extends JpaRepository<FileEntity, String> {

    @Query(value = "SELECT * FROM integration_file f " +
            "WHERE f.job_id = :jobId " +
            "AND f.lob = :lob " +
            "AND (:startTime IS NULL OR f.last_modified_time >= CAST(:startTime AS TIMESTAMP)) " +
            "AND (:endTime IS NULL OR f.last_modified_time <= CAST(:endTime AS TIMESTAMP)) " +
            "ORDER BY f.last_modified_time DESC",
            nativeQuery = true)
    Page<FileEntity> findFilesByJobId(
            @Param("jobId") String jobId,
            @Param("lob") String lob,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            Pageable pageable
    );


    @Query("SELECT f FROM FileEntity f " +
            "WHERE f.isApiBased = true " +
            "  AND (f.consumedStatus = :status OR f.publishedStatus = :status) " +
            "  AND f.lastModifiedTime < :staleCutoffTime " +
            "  AND f.lastModifiedTime >= :tooOldCutoffTime")
    List<FileEntity> findStaleApiBasedPendingFiles(
            @Param("staleCutoffTime") Instant staleCutoffTime,
            @Param("tooOldCutoffTime") Instant tooOldCutoffTime,
            @Param("status") FileStatus status
    );

    Optional<FileEntity> findByFileIdAndMaster(String fileId, String master);

    boolean existsByFileIdAndMaster(String fileId, String master);
}
