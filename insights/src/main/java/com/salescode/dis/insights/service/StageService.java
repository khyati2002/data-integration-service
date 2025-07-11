package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.StageDto;
import com.salescode.dis.insights.dto.StageResponseDTO;
import com.salescode.dis.insights.entity.StageMetadata;
import com.salescode.dis.insights.repository.StageMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StageService {

    private final StageMetadataRepository stageMetadataRepository;

    public List<StageResponseDTO> getStagesByLob() {
        log.info("Fetching stages data grouped by LOB (mode)");

        // Fetch all stage metadata from database
        List<StageMetadata> allStages = stageMetadataRepository.findAll();

        // Group stages by mode
        Map<String, List<StageMetadata>> stagesByMode = allStages.stream()
                .collect(Collectors.groupingBy(stage -> stage.getMode().name()));
        // Convert to response DTOs
        return stagesByMode.entrySet().stream()
                .map(entry -> {
                    String mode = entry.getKey();
                    List<StageDto> stageDTOs = entry.getValue().stream()
                            .map(this::convertToStageDTO)
                            .collect(Collectors.toList());

                    return new StageResponseDTO(mode, stageDTOs);
                })
                .collect(Collectors.toList());
    }

    private StageDto convertToStageDTO(StageMetadata stageMetadata) {
        return new StageDto(
                stageMetadata.getName(),
                stageMetadata.getDescription(),
                stageMetadata.getActionToTake()
        );
    }
}