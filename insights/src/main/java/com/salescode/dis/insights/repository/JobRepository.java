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
    @Query("DELETE FROM JobEntity j WHERE j.lastModifiedTime < :cutoffTime")
    int deleteByLastModifiedBefore(Instant cutoffTime);

    @Query("SELECT DISTINCT j.lob FROM JobEntity j")
    List<String> findDistinctLobs();

}