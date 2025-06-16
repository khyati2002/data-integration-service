package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.FileOperationsHelperService;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiBasedFileOperationStrategy implements IFileOperationStrategy {

    private final FileRepository fileRepository;
    private final JobService jobService;
    private final FileOperationsHelperService fileOperationsHelperService;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity, String jobId) {
        JobEntity job = jobService.createJobIfNotExists(jobId, fileEntity.getLob());
        FileEntity savedFile = fileOperationsHelperService.saveFileEntity(fileEntity, job, this);
        log.info("Registered file {} under job {} for API_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public void updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileEntity;
        if (file == null) {
            log.info("File not found for fileId: {}, master: {}. Creating new file and job if not exists.", fileId, masterName);
            // Create a new file along with the job if not exists
            JobEntity job = jobService.createJobIfNotExists(jobId, lob);

            file = new FileEntity();
            file.setFileId(fileId);
            file.setMaster(masterName);
            file.setLob(lob);
            file.setJob(job);
            file.setModeOfIntegration(ModeOfIntegration.CK_API); // Always API_BASED for this strategy

            // Persist the newly created file entity
            file = fileRepository.save(file);
            job.getFiles().add(file); // Ensure job's file list is updated for totalFileCount on next job update
            log.info("Created new file entity (id: {}) for fileId: {}, master: {} under job: {}", file.getId(), fileId, masterName, jobId);
        }

        fileOperationsHelperService.updateMetrics(file, progress);
        log.info("API_BASED file {} progress updated for stage {}", file.getFileId(), progress.getStageName());
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_API;
    }

    @Override
    public Set<ProgressStage> getSupportedStages() {
        return Set.of();
    }
} 