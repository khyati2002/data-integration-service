package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.IntegrationStageProgress;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.IntegrationMode;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.IntegrationStageProgressRepository;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.dto.StageDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({IntegrationStageService.class, FileIntegrationProcessor.class, ApiIntegrationProcessor.class, MdmIntegrationProcessor.class, JobService.class, FileService.class})
public class FileIntegrationLifecycleTest {

    @Autowired
    private IntegrationStageService integrationStageService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private IntegrationStageProgressRepository stageProgressRepository;

    @Autowired
    private JobService jobService; // To create and manage jobs

    @Autowired
    private FileService fileService; // To create and manage files

    @Autowired
    private FileIntegrationProcessor fileIntegrationProcessor; // To get stage definitions

    private JobEntity testJobEntity;
    private FileEntity testFileEntity;

    private final String LOB = "test_lob";
    private final String MASTER_NAME = "test_master";
    private final String JOB_ID = UUID.randomUUID().toString();
    private final String FILE_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // 1. Create a Job
        testJobEntity = jobService.createJobIfNotExists(JOB_ID, LOB);
        assertThat(testJobEntity).isNotNull();

        // 2. Create a File associated with the Job
        FileEntity initialFile = FileEntity.builder()
                .fileId(FILE_ID)
                .master(MASTER_NAME)
                .lob(LOB) // Assuming lob is set here as well
                .modeOfIntegration(IntegrationMode.FILE)
                .totalCount(100L)
                .job(testJobEntity) // Associate with the job
                .build();
        testFileEntity = fileService.createFile(testJobEntity.getId().toString(), initialFile); // Use job ID to create file
        assertThat(testFileEntity).isNotNull();
        assertThat(testFileEntity.getId()).isNotNull();
    }

    @Test
    void testCompleteFileIntegrationLifecycle() {
        // 3. Initialize Stages for the File
        integrationStageService.initializeStages(testFileEntity);

        List<IntegrationStageProgress> initialProgress = stageProgressRepository.findByFileId(testFileEntity.getId());
        assertThat(initialProgress).hasSize(fileIntegrationProcessor.getStagesForMode().size());
        initialProgress.forEach(stage -> {
            assertThat(stage.getSuccessCount()).isZero();
            assertThat(stage.getFailCount()).isZero();
            assertThat(stage.getIsCompleted()).isFalse();
        });

        // 4. Simulate Progression Through Each Stage
        List<StageDefinition> fileStages = fileIntegrationProcessor.getStagesForMode();

        for (int i = 0; i < fileStages.size(); i++) {
            StageDefinition currentStageDef = fileStages.get(i);
            long successCount = i * 10L; // Simulate increasing success
            long failCount = i * 2L;     // Simulate increasing failures

            integrationStageService.updateStageProgress(
                    testFileEntity.getId(),
                    testFileEntity.getModeOfIntegration(),
                    currentStageDef.getName(),
                    successCount,
                    failCount
            );

            // Verify the current stage's update
            IntegrationStageProgress updatedStage = stageProgressRepository.findByFileIdAndStage(testFileEntity.getId(), currentStageDef.getName())
                    .orElseThrow(() -> new AssertionError("Stage " + currentStageDef.getName() + " not found after update."));

            assertThat(updatedStage.getSuccessCount()).isEqualTo(successCount);
            assertThat(updatedStage.getFailCount()).isEqualTo(failCount);
            assertThat(updatedStage.getIsCompleted()).isTrue();

            // Ensure metadata remains consistent (not updated here)
            assertThat(updatedStage.getDescription()).isEqualTo(currentStageDef.getDescription());
            assertThat(updatedStage.getActionToBeTaken()).isEqualTo(currentStageDef.getActionToBeTaken());
            assertThat(updatedStage.getHowToCheckIssue()).isEqualTo(currentStageDef.getHowToCheckIssue());
        }

        // 5. Mark Overall File as Complete
        // Simulate final processing and update FileEntity statuses
        testFileEntity.setPublishedStatus(FileStatus.COMPLETED);
        testFileEntity.setConsumedStatus(FileStatus.COMPLETED);
        fileRepository.save(testFileEntity);

        // 6. Final Assertions on FileEntity
        FileEntity finalFileEntity = fileRepository.findById(testFileEntity.getId())
                .orElseThrow(() -> new AssertionError("FileEntity not found after completion."));

        assertThat(finalFileEntity.getPublishedStatus()).isEqualTo(FileStatus.COMPLETED);
        assertThat(finalFileEntity.getConsumedStatus()).isEqualTo(FileStatus.COMPLETED);
        assertThat(finalFileEntity.getEndTime()).isNotNull(); // endTime should be set on final status update
    }
} 