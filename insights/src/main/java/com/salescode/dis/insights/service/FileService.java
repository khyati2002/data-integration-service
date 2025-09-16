// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileReportEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.FileReportRepository;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.strategy.IFileOperationStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepo;
    private final List<IFileOperationStrategy> fileOperationStrategies;
    private final FileReportRepository fileReportRepository;

    private Map<ModeOfIntegration, IFileOperationStrategy> operationStrategyMap;

    @PostConstruct
    public void init() {
        operationStrategyMap = fileOperationStrategies.stream()
                .collect(Collectors.toMap(IFileOperationStrategy::getModeOfIntegration, Function.identity(), (existing, replacement) -> existing, // handle duplicates if any, keep the existing one
                        () -> new EnumMap<>(ModeOfIntegration.class)));
    }

    public IFileOperationStrategy getFileOperationStrategy(ModeOfIntegration file) {
        return Optional.ofNullable(operationStrategyMap.get(file))
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for mode of integration: " + file));
    }

    public FileEntity createFile(String jobId, FileEntity file) {
        checkFileIdAlreadyExistsByMasterIfSent(file);
        IFileOperationStrategy strategy = getFileOperationStrategy(file.getModeOfIntegration());
        return strategy.createFile(file, jobId);
    }

    private void checkFileIdAlreadyExistsByMasterIfSent(FileEntity file) {
        if (file.getFileId() != null && fileRepo.existsByFileIdAndMaster(file.getFileId(), Objects.requireNonNull(file.getMaster()))) {
            throw new IllegalArgumentException("File with fileId " + file.getFileId() + ", master " + file.getMaster() + " already exists");
        }
    }

    @Transactional(readOnly = true)
    public FileEntity get(String fileId, String master) {
        return fileRepo.findByFileIdAndMaster(fileId, master)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    @Transactional(readOnly = true)
    public Page<FileEntity> listByJob(String jobId, Pageable pageable) {
        return fileRepo.findByJobId(jobId, pageable);
    }

    @Transactional
    public void updateProgress(String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileRepo.findByFileIdAndMaster(fileId, masterName).orElse(null);
        ModeOfIntegration mode = (file != null) ? file.getModeOfIntegration() :
                (progress.getModeOfIntegration() != null) ? progress.getModeOfIntegration() : ModeOfIntegration.CK_API_CLIENT;
        IFileOperationStrategy strategy = getFileOperationStrategy(mode);
        strategy.updateFileProgress(file, fileId, masterName, jobId, lob, progress);
    }


    public void existsByFileIdAndMaster(String fileId, String masterName) {
        if(!fileRepo.existsByFileIdAndMaster(fileId,masterName)){
            throw new ResourceNotFoundException("File not found: " + fileId);
        }
    }

    public FileEntity setTotalCount(String fileId, String masterName, long totalCount){
        FileEntity file = fileRepo.findByFileIdAndMaster(fileId, masterName)
                .orElseThrow(() -> new EntityNotFoundException("File entity does not exist with fileId: " + fileId + " and master: " + masterName));
        file.setTotalCount(totalCount);
        return file;
    }

    public List<ModeOfIntegration> getAllModesOfIntegration() {
        return fileOperationStrategies.stream()
                .map(IFileOperationStrategy::getModeOfIntegration)
                .collect(Collectors.toList());
    }

    public FileReportEntity createReportEntry(String fileId) {
        FileReportEntity report = new FileReportEntity();
        report.setFileId(fileId);
        report.setName(null);
        report.setStatus("IN_PROGRESS");
        return fileReportRepository.save(report);
    }

    public FileEntity updateStatus(String fileId, String masterName, ProgressStatus status) {
        FileEntity file = fileRepo.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        file.setStatus(status);
        return fileRepo.save(file);
    }

    @Transactional
    public int updateStaleFiles(String lob, ProgressStatus currentStatus,
                                ProgressStatus newStatus, Instant cutoffTime) {

        log.debug("Updating stale files for LOB: {}, currentStatus: {}, newStatus: {}, cutoffTime: {}",
                lob, currentStatus, newStatus, cutoffTime);

        int updatedCount = fileRepo.updateStaleFilesByLobAndStatus(
                lob, currentStatus, newStatus, cutoffTime);

        log.debug("Successfully updated {} files from {} to {} for LOB: {}",
                updatedCount, currentStatus, newStatus, lob);

        return updatedCount;
    }




}