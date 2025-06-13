package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.StageInfo;
import com.salescode.dis.insights.entity.StageMetadata;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.repository.StageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageRegistry {
    private final StageMetadataRepository stageMetadataRepository;

    public List<ModeOfIntegration> getSupportedModes() {
        return stageMetadataRepository.findAll().stream()
                .map(StageMetadata::getMode)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<StageInfo> getStagesForMode(ModeOfIntegration mode) {
        return stageMetadataRepository.findByMode(mode).stream()
                .map(meta -> new StageInfo(meta.getStageName(), meta.getDescription(), meta.getActionToTake()))
                .collect(Collectors.toList());
    }

    public Map<ModeOfIntegration, List<StageInfo>> getAllStages() {
        List<StageMetadata> all = stageMetadataRepository.findAll();
        return all.stream().collect(Collectors.groupingBy(
                StageMetadata::getMode,
                () -> new EnumMap<>(ModeOfIntegration.class),
                Collectors.mapping(meta -> new StageInfo(meta.getStageName(), meta.getDescription(), meta.getActionToTake()), Collectors.toList())
        ));
    }
} 