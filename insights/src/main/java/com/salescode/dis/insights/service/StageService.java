package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.StageDto;
import com.salescode.dis.insights.entity.StageMetadata;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.repository.StageMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StageService {

    private final StageMetadataRepository stageMetadataRepository;
    private final FileStageMetricsRepository fileStageMetricsRepository;

    public List<StageDto> getStages() {
        List<StageMetadata> allStages = stageMetadataRepository.findAll();

        return allStages.stream()
                .map(this::convertToStageDTO)
                .collect(Collectors.toList());
    }

    private StageDto convertToStageDTO(StageMetadata stageMetadata) {
        return new StageDto(
                stageMetadata.getMode().name(), // Include mode in the DTO
                stageMetadata.getStageType(),
                stageMetadata.getDescription(),
                stageMetadata.getActionToTake()
        );
    }

    @Transactional
    public int updateStaleStageMetrics(String lob, ProgressStatus currentStatus,
                                       ProgressStatus newStatus, Instant cutoffTime) {

        log.debug("Updating stale stage metrics for LOB: {}, currentStatus: {}, newStatus: {}, cutoffTime: {}",
                lob, currentStatus, newStatus, cutoffTime);

        int updatedCount = fileStageMetricsRepository.updateStaleStageMetricsByLobAndStatus(
                lob, currentStatus, newStatus, cutoffTime);

        log.debug("Successfully updated {} stage metrics from {} to {} for LOB: {}",
                updatedCount, currentStatus, newStatus, lob);

        return updatedCount;
    }
}