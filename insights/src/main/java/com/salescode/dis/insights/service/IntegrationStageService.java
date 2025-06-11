package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.IntegrationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IntegrationStageService {

    private final Map<IntegrationMode, IntegrationProcessor> processors;

    @Autowired
    public IntegrationStageService(List<IntegrationProcessor> integrationProcessors) {
        this.processors = integrationProcessors.stream()
                .collect(Collectors.toMap(IntegrationProcessor::getSupportedMode, Function.identity()));
    }

    public void initializeStages(FileEntity file) {
        IntegrationProcessor processor = getProcessor(file.getModeOfIntegration());
        processor.initializeStages(file);
    }

    public void updateStageProgress(Long fileId, IntegrationMode modeOfIntegration, String stage, Long successCount, Long failCount) {
        IntegrationProcessor processor = getProcessor(modeOfIntegration);
        processor.updateStageProgress(fileId, stage, successCount, failCount);
    }

    private IntegrationProcessor getProcessor(IntegrationMode mode) {
        IntegrationProcessor processor = processors.get(mode);
        if (processor == null) {
            throw new IllegalArgumentException("No integration processor found for mode: " + mode);
        }
        return processor;
    }
} 