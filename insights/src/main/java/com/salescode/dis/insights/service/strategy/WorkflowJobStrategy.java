package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class WorkflowJobStrategy implements IFileOperationStrategy{
    private final FileRepository fileRepository;

    public WorkflowJobStrategy(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public FileEntity createFile(FileEntity fileEntity, String jobId) {
        return fileRepository.save(fileEntity);
    }

    @Override
    public FileStageMetrics updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress) {
        return null;
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
