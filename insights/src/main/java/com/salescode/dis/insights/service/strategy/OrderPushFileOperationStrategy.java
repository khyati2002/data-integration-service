package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.service.FileOperationsHelperService;
import com.salescode.dis.insights.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.salescode.dis.insights.enums.ProgressStage.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPushFileOperationStrategy implements IFileOperationStrategy {

    private final JobService jobService;
    private final FileOperationsHelperService fileOperationsHelperService;

    @Override
    @Transactional
    public  FileEntity createFile(FileEntity fileEntity, String jobId) {
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
        if (getSupportedStages().get(0).equals(fileStageMetrics.getStageType())) {
            file.setTotalCount(fileStageMetrics.getTotal());
        }
        log.info("ORDER_PUSH file {} progress updated for stage {}", file.getFileId(), progress.getStageType());
        return fileStageMetrics;
    }
    @Override
    public ModeOfIntegration getModeOfIntegration() {
        return ModeOfIntegration.CK_ORDER_PUSH;

    }

    @Override
    public List<ProgressStage> getSupportedStages() {
        return List.of(READ, PUBLISH, SAVE);
    }
}