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
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, String> {

    Page<JobEntity> getJobEntitiesByLob(String lob, Pageable pageable);

    List<JobEntity> findByLob(String lob);

    int countByLobAndStatus(String lob, ProgressStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobEntity j WHERE j.lastModifiedTime < :cutoffTime")
    int deleteByLastModifiedBefore(Instant cutoffTime);

    @Query(value = "SELECT id FROM integration_job WHERE last_modified < ?1", nativeQuery = true)
    List<String> findJobIdsOlderThan(Instant cutoffTime);

    @Modifying
    @Query(value = "DELETE FROM integration_job WHERE id IN (?1)", nativeQuery = true)
    int deleteByIdIn(List<String> jobIds);


    @Modifying
    @Transactional
    @Query("UPDATE JobEntity j SET j.status = :newStatus, j.lastModifiedTime = CURRENT_TIMESTAMP " +
            "WHERE j.lob = :lob AND j.status = :currentStatus " +
            "AND j.lastModifiedTime < :cutoffTime")
    int updateStaleintegration_jobByLobAndStatus(@Param("lob") String lob,
                                      @Param("currentStatus") ProgressStatus currentStatus,
                                      @Param("newStatus") ProgressStatus newStatus,
                                      @Param("cutoffTime") Instant cutoffTime);
}