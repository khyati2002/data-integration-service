package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, String> {
    Page<FileEntity> findByJobId(String jobId, Pageable pageable);
}