package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
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

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM integration_file f " +
                    "WHERE f.id IN (" +
                    "  SELECT f2.id FROM integration_file f2 " +
                    "  LEFT JOIN file_stage_metrics fsm ON fsm.file_id = f2.id " +
                    "  WHERE f2.last_modified_time < :cutoffTime AND fsm.file_id IS NULL" +
                    ")",
            nativeQuery = true
    )
    int deleteByLastModifiedBefore(@Param("cutoffTime") Instant cutoffTime);


    @Modifying
    @Transactional
    @Query("UPDATE FileEntity f SET f.status = :newStatus, f.lastModifiedTime = CURRENT_TIMESTAMP " +
            "WHERE f.lob = :lob AND f.status = :currentStatus " +
            "AND f.lastModifiedTime < :cutoffTime")
    int updateStaleFilesByLobAndStatus(@Param("lob") String lob,
                                       @Param("currentStatus") ProgressStatus currentStatus,
                                       @Param("newStatus") ProgressStatus newStatus,
                                       @Param("cutoffTime") Instant cutoffTime);


}
