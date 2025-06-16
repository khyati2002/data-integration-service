package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ModeOfIntegration;

import java.util.Set;

public interface IFileOperationStrategy {
    FileEntity createFile(FileEntity fileEntity, String jobId);
    void updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress);
    ModeOfIntegration getModeOfIntegration();
    Set<ProgressStage> getSupportedStages();
    // Add other methods as needed, e.g., getFileDetails, updateFileStatus (if still relevant in new context)
} 