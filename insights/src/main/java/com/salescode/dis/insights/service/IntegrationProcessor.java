package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.IntegrationMode;

public interface IntegrationProcessor {
    void initializeStages(FileEntity file);
    void updateStageProgress(Long fileId, String stage, Long successCount, Long failCount);
    IntegrationMode getSupportedMode();
} 