package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface JobRepository extends JpaRepository<JobEntity, String> {

    Page<JobEntity> getJobEntitiesByLob(String lob, Pageable pageable);

    List<JobEntity> findByLob(String lob);

    int countByLobAndStatus(String lob, ProgressStatus status);

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM integration_job j WHERE j.id IN (" +
                    "  SELECT j2.id FROM integration_job j2 " +
                    "  LEFT JOIN file_stage_metrics fsm ON fsm.job_id = j2.id " +
                    "  LEFT JOIN integration_file ifl ON ifl.job_id = j2.id " +
                    "  WHERE j2.last_modified_time < :cutoffTime " +
                    "    AND fsm.job_id IS NULL " +
                    "    AND ifl.job_id IS NULL" +
                    ")",
            nativeQuery = true
    )
    int deleteByLastModifiedBefore(@Param("cutoffTime") Instant cutoffTime);




    @Modifying
    @Transactional
    @Query("UPDATE JobEntity j SET j.status = :newStatus, j.lastModifiedTime = CURRENT_TIMESTAMP " +
            "WHERE j.lob = :lob AND j.status = :currentStatus " +
            "AND j.lastModifiedTime < :cutoffTime")
    int updateStaleJobsByLobAndStatus(@Param("lob") String lob,
                                      @Param("currentStatus") ProgressStatus currentStatus,
                                      @Param("newStatus") ProgressStatus newStatus,
                                      @Param("cutoffTime") Instant cutoffTime);
}