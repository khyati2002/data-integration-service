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

    public void updateProgress(String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileRepo.findByFileIdAndMaster(fileId, masterName).orElse(null);

        // Determine mode of integration: from existing file or default to API_BASED if not found
        ModeOfIntegration mode = null;
        if (file != null) {
            mode = file.getModeOfIntegration();
        }

        if (mode == null) {
            mode = ModeOfIntegration.API_BASED; // Default if file doesn't exist or mode is null
        }

        ModeOfIntegration finalMode = mode;
        IFileOperationStrategy strategy = Optional.ofNullable(operationStrategyMap.get(finalMode))
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for mode of integration: " + finalMode));

        // Validate stageName against the registry for the determined mode
        String stageName = progress.getStageName();
        boolean validStage = stageRegistry.getStagesForMode(finalMode).stream()
                .anyMatch(info -> info.getStageName().equals(stageName));
        if (!validStage) {
            throw new IllegalArgumentException("Invalid stage for " + finalMode + " integration: " + stageName);
        }

        // Pass the fetched file (which can be null) and all other relevant information to the strategy
        strategy.updateFileProgress(file, fileId, masterName, jobId, lob, progress);
    }


}