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

public interface FileRepository extends JpaRepository<FileEntity, String> {
    Page<FileEntity> findByJobId(String jobId, Pageable pageable);

    @Query("SELECT f FROM FileEntity f WHERE f.isApiBased = true AND f.lastModifiedTime > :cutoffTime AND (f.consumedStatus = :status OR f.publishedStatus = :status)")
    List<FileEntity> findApiBasedFilesModifiedSince(@Param("cutoffTime") Instant cutoffTime, @Param("status") FileStatus status);
}
