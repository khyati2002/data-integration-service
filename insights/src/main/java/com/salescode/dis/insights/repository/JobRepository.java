package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface JobRepository extends JpaRepository<JobEntity, String>, JpaSpecificationExecutor<JobEntity> {

    Page<JobEntity> getJobEntitiesByLob(String lob, Pageable pageable);

    @Query(value = "SELECT igj.status, igf.master, igj.start_time, igj.end_time, igj.id AS job_id, " +
            "igf.file_id AS file_id, igj.lob, igf.published_success_count, igf.published_fail_count, " +
            "igf.consumed_fail_count, igf.consumed_success_count, igf.total_count, " +
            "igf.publisher_throughput, igf.consumer_throughput, igf.server_fail_count, igf.logical_fail_count " +
            "FROM integration_job igj " +
            "LEFT JOIN integration_file igf ON igj.id = igf.job_id " +
            "WHERE (COALESCE(:lob) IS NULL OR igj.lob IN (:lob)) " +
            "AND (COALESCE(:status) IS NULL OR igj.status IN (:status)) " +
            "AND (COALESCE(:startTime) IS NULL OR igj.last_modified_time >= :startTime) " +
            "AND (COALESCE(:endTime) IS NULL OR igj.last_modified_time <= :endTime)",
            nativeQuery = true)
    List<Map<String, Object>> getLobSummary(
            @Param("lob") List<String> lob,
            @Param("status") List<String> status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);




//    @Query(
//            value = "SELECT igj.status, igj.master, igj.start_time,igj.end_time,igj.id AS job_id, igf.id AS file_id, igj.lob, igj.status, " +
//                    "igf.consumed_fail_count, igf.consumed_success_count, igf.published_success_count,igf.published_fail_count,igf.total_count, igf.publisher_throughput,igf.consumer_throughput, igf.server_fail_count,igf.logical_fail_count " +
//                    "FROM integration_job igj " +
//                    "LEFT JOIN integration_file igf ON igj.id = igf.job_id ",
//            nativeQuery = true)
//    List<Map<String, Object>> getLobSummaryAll();

    @Query("SELECT j.lob AS lob, COUNT(j) AS total, " +
            "SUM(CASE WHEN j.status = 'COMPLETED_SUCCESSFULLY' OR j.status = 'COMPLETED_WITH_FAILURES' THEN 1 ELSE 0 END) AS COMPLETED, "+
            "SUM(CASE WHEN j.status = 'PENDING' THEN 1 ELSE 0 END) AS PENDING, " +
            "SUM(CASE WHEN j.status = 'FAILED' THEN 1 ELSE 0 END) AS FAILED " +
            "FROM JobEntity j " +
            "WHERE j.lastModifiedTime BETWEEN :startTime AND :endTime " +
            "GROUP BY j.lob")
    List<Map<String, Object>> getLobDetailsAll(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );



    @Query("SELECT j.lob AS lob, COUNT(j) AS total, " +
            "SUM(CASE WHEN j.status = 'COMPLETED_SUCCESSFULLY' OR j.status = 'COMPLETED_WITH_FAILURES' THEN 1 ELSE 0 END) AS COMPLETED, " +
            "SUM(CASE WHEN j.status = 'PENDING' THEN 1 ELSE 0 END) AS PENDING, " +
            "SUM(CASE WHEN j.status = 'FAILED' THEN 1 ELSE 0 END) AS FAILED " +
            "FROM JobEntity j " +
            "WHERE j.lob IN :lob AND j.lastModifiedTime BETWEEN :startTime AND :endTime " +
            "GROUP BY j.lob")
    List<Map<String, Object>> getLobDetails(
            @Param("lob") List<String> lob,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );


}