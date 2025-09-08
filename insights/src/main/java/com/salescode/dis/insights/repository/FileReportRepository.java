package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.FileReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileReportRepository extends JpaRepository<FileReportEntity, UUID> {

    Optional<FileReportEntity> findByFileId(String fileId);

}