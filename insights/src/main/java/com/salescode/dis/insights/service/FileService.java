// File: service/FileService.java
package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ApiBasedStages;
import com.salescode.dis.insights.enums.FileBasedStages;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.strategy.IFileOperationStrategy;
import com.salescode.dis.insights.service.StageRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepo;
    private final JobService jobService;
    private final List<IFileOperationStrategy> fileOperationStrategies;
    private final StageRegistry stageRegistry;

    private Map<ModeOfIntegration, IFileOperationStrategy> operationStrategyMap;

    @PostConstruct
    public void init() {
        operationStrategyMap = fileOperationStrategies.stream()
                .collect(Collectors.toMap(IFileOperationStrategy::getModeOfIntegration, Function.identity(),
                        (existing, replacement) -> existing, // handle duplicates if any, keep the existing one
                        () -> new EnumMap<>(ModeOfIntegration.class)));
    }

    public FileEntity createFile(String jobId, FileEntity file) {
        checkFileIdAlreadyExistsByMasterIfSent(file);
        if (file.getModeOfIntegration() == null) {
            file.setModeOfIntegration(ModeOfIntegration.API_BASED); // Default if not provided
        }
        IFileOperationStrategy strategy = Optional.ofNullable(operationStrategyMap.get(file.getModeOfIntegration()))
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for mode of integration: " + file.getModeOfIntegration()));
        return strategy.createFile(file);
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

    public void updateProgress(String fileId, String masterName, FileProgressRequest progress) {
        FileEntity file = get(fileId, masterName);
        ModeOfIntegration mode = file.getModeOfIntegration();
        if (mode == null) {
            // If modeOfIntegration is not set in the file, default to API_BASED
            mode = ModeOfIntegration.API_BASED;
            file.setModeOfIntegration(mode);
        }
        ModeOfIntegration finalMode = mode;
        IFileOperationStrategy strategy = Optional.ofNullable(operationStrategyMap.get(mode))
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for mode of integration: " + finalMode));

        // Validate stageName against the registry
        String stageName = progress.getStageName();
        boolean validStage = stageRegistry.getStagesForMode(mode).stream()
                .anyMatch(info -> info.getStageName().equals(stageName));
        if (!validStage) {
            throw new IllegalArgumentException("Invalid stage for " + mode + " integration: " + stageName);
        }
        strategy.updateFileProgress(file, stageName, progress.getSuccessCount(), progress.getFailureCount(), progress.getMinProcessingTimeMs(), progress.getMaxProcessingTimeMs());
    }


}