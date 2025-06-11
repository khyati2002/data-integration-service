package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.IntegrationStageProgress;
import com.salescode.dis.insights.enums.IntegrationMode;
import com.salescode.dis.insights.repository.IntegrationStageProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import com.salescode.dis.insights.dto.StageDefinition;

@Service
@RequiredArgsConstructor
public class MdmIntegrationProcessor implements IntegrationProcessor {

    private final IntegrationStageProgressRepository stageProgressRepository;

    @Override
    @Transactional
    public void initializeStages(FileEntity file) {
        List<StageDefinition> stages = getStagesForMode();

        for (StageDefinition stageDef : stages) {
            IntegrationStageProgress progress = IntegrationStageProgress.builder()
                    .file(file)
                    .stage(stageDef.getName())
                    .description(stageDef.getDescription())
                    .actionToBeTaken(stageDef.getActionToBeTaken())
                    .howToCheckIssue(stageDef.getHowToCheckIssue())
                    .successCount(0L)
                    .failCount(0L)
                    .isCompleted(false)
                    .build();
            stageProgressRepository.save(progress);
        }
    }

    @Override
    @Transactional
    public void updateStageProgress(Long fileId, String stage, Long successCount, Long failCount) {
        IntegrationStageProgress progress = stageProgressRepository.findByFileIdAndStage(fileId, stage)
                .orElseThrow(() -> new RuntimeException("Stage progress not found for file: " + fileId + " and stage: " + stage));

        progress.setSuccessCount(successCount);
        progress.setFailCount(failCount);
        progress.setIsCompleted(true);

        stageProgressRepository.save(progress);
    }

    @Override
    public IntegrationMode getSupportedMode() {
        return IntegrationMode.MDM;
    }

    private List<StageDefinition> getStagesForMode() {
        return Arrays.asList(
                StageDefinition.builder()
                        .name("INITIALIZATION")
                        .description("Initializing the MDM integration process.")
                        .actionToBeTaken("Verify MDM system connectivity and credentials.")
                        .howToCheckIssue("Check application logs for MDM initialization errors.")
                        .build(),
                StageDefinition.builder()
                        .name("MDM_CONNECTION")
                        .description("Establishing connection to the Master Data Management system.")
                        .actionToBeTaken("Ensure MDM system is online and accessible.")
                        .howToCheckIssue("Inspect network logs and MDM client connection logs.")
                        .build(),
                StageDefinition.builder()
                        .name("MDM_DATA_SYNC")
                        .description("Synchronizing data with the MDM system.")
                        .actionToBeTaken("Review MDM data models and mapping configurations.")
                        .howToCheckIssue("Examine MDM synchronization logs for data conflicts.")
                        .build(),
                StageDefinition.builder()
                        .name("MDM_VALIDATION")
                        .description("Validating data against MDM rules and policies.")
                        .actionToBeTaken("Correct invalid master data entries.")
                        .howToCheckIssue("Review MDM validation reports and rejected records.")
                        .build(),
                StageDefinition.builder()
                        .name("MDM_PROCESSING")
                        .description("Processing and applying data changes within the MDM system.")
                        .actionToBeTaken("Address MDM internal processing errors.")
                        .howToCheckIssue("Check MDM system logs for data processing failures.")
                        .build(),
                StageDefinition.builder()
                        .name("COMPLETION")
                        .description("Finalizing the MDM integration process.")
                        .actionToBeTaken("Confirm data integrity in the MDM system.")
                        .howToCheckIssue("Verify final reconciliation reports from MDM.")
                        .build()
        );
    }
} 