package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.service.FileOperationsHelperService;
import com.salescode.dis.insights.service.FileService;
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
public class ApiClientBasedFileOperationStrategy implements IFileOperationStrategy {

    private final JobService jobService;
    private final FileOperationsHelperService fileOperationsHelperService;

    @Override
    @Transactional
    public FileEntity createFile(FileEntity fileEntity, String jobId) {
        throw new UnsupportedOperationException("Not supported for client based api integrations");
    }

    @Override
    @Transactional
    public FileStageMetrics updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        FileEntity file = fileEntity;
        if (file == null) {
            log.info("File not found for fileId: {}, master: {}. Creating new file and job if not exists.", fileId, masterName);
            // Create a new file along with the job if not exists
            JobEntity job = jobService.createJobIfNotExists(jobId, lob, getModeOfIntegration());
            file = new FileEntity();
            file.setFileId(fileId);
            file.setMaster(masterName);
            file.setLob(lob);
            file.setModeOfIntegration(progress.getModeOfIntegration());
            file = fileOperationsHelperService.saveFileEntity(file, job, this);
        }
        FileStageMetrics fileStageMetrics = fileOperationsHelperService.updateMetrics(file, progress);
        if (getSupportedStages().getFirst().equals(fileStageMetrics.getStageType())) {
            file.setTotalCount(fileStageMetrics.getTotal());
        }
        log.info("API_BASED file {} progress updated for stage {}", file.getFileId(), progress.getStageType());
        return fileStageMetrics;
    }

    @Transactional
    public void updateStatus(FileStageMetrics fileStageMetrics) {
        FileEntity file = fileStageMetrics.getFile();
        if(fileStageMetrics.getTotal() != 0 && fileStageMetrics.getTotal().compareTo(file.getTotalCount()) == 0 ){
            fileStageMetrics.setProgressStatus(fileStageMetrics.getCurrentStatus());
            if(getSupportedStages().getLast().equals(fileStageMetrics.getStageType())){
                file.setStatus(fileStageMetrics.getCurrentStatus());
            }
        }
        else{
            fileStageMetrics.setProgressStatus(ProgressStatus.FAILED);
        }
    }

    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_API_CLIENT;
    }

    @Override
    public List<ProgressStage> getSupportedStages() {
        return List.of(QUEUE, PROCESS, SAVE);
    }
}