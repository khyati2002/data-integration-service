package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.service.FileOperationsHelperService;
import com.salescode.dis.insights.service.JobService;
import com.salescode.dis.insights.validation.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.salescode.dis.insights.enums.ProgressStage.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiBasedFileOperationStrategy implements IFileOperationStrategy {

    private final JobService jobService;
    private final FileOperationsHelperService fileOperationsHelperService;
    private final ValidationService validationService;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity, String jobId) {
        JobEntity job = jobService.getJob(jobId);
        validationService.validate(job, fileEntity);
        FileEntity savedFile = fileOperationsHelperService.saveFileEntity(fileEntity, job, this);
        log.info("Registered file {} under job {} for API_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public FileStageMetrics updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileStageMetrics fileStageMetrics = fileOperationsHelperService.updateMetrics(fileEntity, progress);
        if(getSupportedStages().getFirst().equals(fileStageMetrics.getStageType())){
            fileEntity.setTotalCount(fileStageMetrics.getTotal());
        }
        log.info("API_BASED file {} progress updated for stage {}", fileEntity.getFileId(), progress.getStageType());
        return fileStageMetrics;
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_API;
    }

    @Override
    public List<ProgressStage> getSupportedStages() {
        return List.of(READ, PUBLISH, QUEUE, PROCESS, SAVE);
    }
} 