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
public class ApiIntegrationProcessor implements IntegrationProcessor {

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
        return IntegrationMode.API;
    }

    private List<StageDefinition> getStagesForMode() {
        return Arrays.asList(
                StageDefinition.builder()
                        .name("INITIALIZATION")
                        .description("Initializing the API integration process.")
                        .actionToBeTaken("Verify API endpoint availability and configuration.")
                        .howToCheckIssue("Check application logs for API initialization errors.")
                        .build(),
                StageDefinition.builder()
                        .name("API_AUTHENTICATION")
                        .description("Authenticating with the external API.")
                        .actionToBeTaken("Review API credentials and token validity.")
                        .howToCheckIssue("Inspect authentication service logs and API gateway logs.")
                        .build(),
                StageDefinition.builder()
                        .name("API_REQUEST")
                        .description("Sending data requests to the external API.")
                        .actionToBeTaken("Check API request payload and parameters.")
                        .howToCheckIssue("Review network traffic logs and API client logs.")
                        .build(),
                StageDefinition.builder()
                        .name("API_RESPONSE_PROCESSING")
                        .description("Processing the response received from the API.")
                        .actionToBeTaken("Verify API response format and data structure.")
                        .howToCheckIssue("Examine API response logs and data transformation errors.")
                        .build(),
                StageDefinition.builder()
                        .name("API_VALIDATION")
                        .description("Validating the data from API response against business rules.")
                        .actionToBeTaken("Correct invalid data entries or adjust validation rules.")
                        .howToCheckIssue("Review validation reports for rejected API records.")
                        .build(),
                StageDefinition.builder()
                        .name("COMPLETION")
                        .description("Finalizing the API integration process.")
                        .actionToBeTaken("Confirm data consistency in the target system.")
                        .howToCheckIssue("Verify overall API integration status and target system reports.")
                        .build()
        );
    }
} 