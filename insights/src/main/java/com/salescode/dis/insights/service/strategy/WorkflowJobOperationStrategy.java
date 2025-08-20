package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.service.FileOperationsHelperService;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.salescode.dis.insights.enums.ProgressStage.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowJobOperationStrategy implements IFileOperationStrategy {

    private final JobService jobService;
    private final FileOperationsHelperService fileOperationsHelperService;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity, String jobId) {
        JobEntity job = jobService.getJob(jobId);
        FileEntity savedFile = fileOperationsHelperService.saveFileEntity(fileEntity, job, this);
        log.info("Registered file {} under job {} for FILE_BASED integration", savedFile.getId(), job.getId());
        return savedFile;
    }

    @Override
    @Transactional
    public FileStageMetrics updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileEntity;
        fileId = Optional.ofNullable(fileId)
                .orElse(UUID.randomUUID().toString());

        if (file == null) {
            log.info("File not found for fileId: {}, master: {}. Creating new file and job if not exists.", fileId, masterName);
            // Create a new file along with the job if not exists
            JobEntity job = jobService.createWorkflowJob(jobId, lob, getModeOfIntegration(), ProgressStatus.COMPLETED_SUCCESSFULLY);
            file = new FileEntity();
            file.setFileId(fileId);
            file.setMaster(Optional.ofNullable(masterName).orElse("unknown"));
            file.setLob(lob);
            file.setModeOfIntegration(progress.getModeOfIntegration());
            file = fileOperationsHelperService.saveFileEntity(file, job, this);
        }
        FileStageMetrics fileStageMetrics = new FileStageMetrics();
//        if (getSupportedStages().get(0).equals(fileStageMetrics.getStageType())) {
//            file.setTotalCount(fileStageMetrics.getTotal());
//        }
        return fileStageMetrics;
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_WORKFLOW_JOB;
    }

    @Override
    public List<ProgressStage> getSupportedStages() {
        return List.of();
    }
}