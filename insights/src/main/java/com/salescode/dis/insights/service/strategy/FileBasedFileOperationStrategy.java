package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ModeOfIntegration;
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
public class FileBasedFileOperationStrategy implements IFileOperationStrategy {

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
        FileStageMetrics fileStageMetrics = fileOperationsHelperService.updateMetrics(fileEntity, progress);
        log.info("FILE_BASED file {} progress updated for stage {}", fileEntity.getFileId(), progress.getStageType());
        updateStatus(fileEntity, fileStageMetrics);
        return fileStageMetrics;
    }

    @Transactional
    public void updateStatus(FileEntity file, FileStageMetrics fileStageMetrics) {
        if(file.getTotalCount() != 0 && fileStageMetrics.getTotal().compareTo(file.getTotalCount()) ==0 ){
            fileStageMetrics.setProgressStatus(fileStageMetrics.getCurrentStatus());
        }

    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_FILE;
    }

    @Override
    public List<ProgressStage> getSupportedStages() {
        return List.of(READ, PUBLISH, QUEUE, PROCESS, SAVE);
    }
}