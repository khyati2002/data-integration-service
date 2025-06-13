package com.salescode.dis.insights.service.strategy;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;

public interface IFileOperationStrategy {
    FileEntity createFile(FileEntity fileEntity);
    void updateFileProgress(FileEntity fileEntity, String fileId, String masterName, String jobId, String lob, FileProgressRequest progress);
    ModeOfIntegration getModeOfIntegration();
    // Add other methods as needed, e.g., getFileDetails, updateFileStatus (if still relevant in new context)
} 