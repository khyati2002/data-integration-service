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
public class FileIntegrationProcessor implements IntegrationProcessor {

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
        return IntegrationMode.FILE;
    }

    private List<StageDefinition> getStagesForMode() {
        return Arrays.asList(
                StageDefinition.builder()
                        .name("INITIALIZATION")
                        .description("Initializing the file integration process.")
                        .actionToBeTaken("Verify input file integrity.")
                        .howToCheckIssue("Check application logs for initialization errors.")
                        .build(),
                StageDefinition.builder()
                        .name("FILE_UPLOAD")
                        .description("Uploading the file to the designated storage.")
                        .actionToBeTaken("Ensure correct file path and permissions.")
                        .howToCheckIssue("Inspect file storage service logs.")
                        .build(),
                StageDefinition.builder()
                        .name("FILE_PARSING")
                        .description("Parsing the uploaded file content.")
                        .actionToBeTaken("Check file format and encoding.")
                        .howToCheckIssue("Review parsing service logs for format errors.")
                        .build(),
                StageDefinition.builder()
                        .name("FILE_VALIDATION")
                        .description("Validating the parsed file data against business rules.")
                        .actionToBeTaken("Correct invalid data entries.")
                        .howToCheckIssue("Examine validation reports and rejected records.")
                        .build(),
                StageDefinition.builder()
                        .name("FILE_PROCESSING")
                        .description("Processing and inserting validated file data into the target system.")
                        .actionToBeTaken("Address database connection issues or target system errors.")
                        .howToCheckIssue("Check database transaction logs and target system error logs.")
                        .build(),
                StageDefinition.builder()
                        .name("COMPLETION")
                        .description("Finalizing the file integration process.")
                        .actionToBeTaken("Confirm data consistency in the target system.")
                        .howToCheckIssue("Verify final reconciliation reports.")
                        .build()
        );
    }
} 