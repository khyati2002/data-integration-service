// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
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

    private Map<ModeOfIntegration, IFileOperationStrategy> operationStrategyMap;

    @PostConstruct
    public void init() {
        operationStrategyMap = fileOperationStrategies.stream()
                .collect(Collectors.toMap(IFileOperationStrategy::getModeOfIntegration, Function.identity(), (existing, replacement) -> existing, // handle duplicates if any, keep the existing one
                        () -> new EnumMap<>(ModeOfIntegration.class)));
    }

    private IFileOperationStrategy getFileOperationStrategy(ModeOfIntegration file) {
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
        ModeOfIntegration mode = (file != null) ? file.getModeOfIntegration() : ModeOfIntegration.CK_API_CLIENT;
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
}