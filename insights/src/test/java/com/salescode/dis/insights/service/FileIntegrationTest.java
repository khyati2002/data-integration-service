package com.salescode.dis.insights.service;

import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.IntegrationStageProgress;
import com.salescode.dis.insights.enums.IntegrationMode;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.IntegrationStageProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({IntegrationStageService.class, FileIntegrationProcessor.class, ApiIntegrationProcessor.class, MdmIntegrationProcessor.class})
public class FileIntegrationTest {

    @Autowired
    private IntegrationStageService integrationStageService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private IntegrationStageProgressRepository stageProgressRepository;

    private FileEntity testFileEntity;

    @BeforeEach
    void setUp() {
        testFileEntity = FileEntity.builder()
                .fileId("test-file-123")
                .master("customer")
                .modeOfIntegration(IntegrationMode.FILE)
                .totalCount(100L) // Assuming a total count for the file
                .build();
        fileRepository.save(testFileEntity);
    }

    @Test
    void testInitializeStagesForFileMode() {
        // Act
        integrationStageService.initializeStages(testFileEntity);

        // Assert
        List<IntegrationStageProgress> progressRecords = stageProgressRepository.findByFileId(testFileEntity.getId());
        assertThat(progressRecords).hasSize(6); // INITIALIZATION, FILE_UPLOAD, FILE_PARSING, FILE_VALIDATION, FILE_PROCESSING, COMPLETION

        // Verify initial state of a specific stage
        Optional<IntegrationStageProgress> initStage = progressRecords.stream()
                .filter(p -> p.getStage().equals("INITIALIZATION"))
                .findFirst();
        assertThat(initStage).isPresent();
        assertThat(initStage.get().getSuccessCount()).isZero();
        assertThat(initStage.get().getFailCount()).isZero();
        assertThat(initStage.get().getIsCompleted()).isFalse();
        assertThat(initStage.get().getDescription()).isEqualTo("Initializing the file integration process.");
        assertThat(initStage.get().getActionToBeTaken()).isEqualTo("Verify input file integrity.");
        assertThat(initStage.get().getHowToCheckIssue()).isEqualTo("Check application logs for initialization errors.");
    }

    @Test
    void testUpdateStageProgressSuccess() {
        // Arrange: Initialize stages first
        integrationStageService.initializeStages(testFileEntity);

        String stageToUpdate = "FILE_UPLOAD";
        Long successCount = 90L;
        Long failCount = 10L;

        // Act
        integrationStageService.updateStageProgress(
                testFileEntity.getId(),
                testFileEntity.getModeOfIntegration(),
                stageToUpdate,
                successCount,
                failCount
        );

        // Assert
        Optional<IntegrationStageProgress> updatedStage = stageProgressRepository.findByFileIdAndStage(testFileEntity.getId(), stageToUpdate);
        assertThat(updatedStage).isPresent();
        assertThat(updatedStage.get().getSuccessCount()).isEqualTo(successCount);
        assertThat(updatedStage.get().getFailCount()).isEqualTo(failCount);
        assertThat(updatedStage.get().getIsCompleted()).isTrue();

        // Ensure metadata is not changed by update (it's set during initialization)
        assertThat(updatedStage.get().getDescription()).isEqualTo("Uploading the file to the designated storage.");
        assertThat(updatedStage.get().getActionToBeTaken()).isEqualTo("Ensure correct file path and permissions.");
        assertThat(updatedStage.get().getHowToCheckIssue()).isEqualTo("Inspect file storage service logs.");
    }

    @Test
    void testUpdateStageProgressFailureNotFound() {
        // Arrange: Stages are not initialized for this file entity
        // Note: For this test, the file entity exists, but the stages for it are not initialized,
        // so finding by fileId and stage should result in an error.
        integrationStageService.initializeStages(testFileEntity); // Initialize stages so fileId exists in stageProgressRepository context

        Long existingFileId = testFileEntity.getId();
        String stageToUpdate = "NON_EXISTENT_STAGE"; // A stage that's definitely not initialized for FILE mode

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            integrationStageService.updateStageProgress(
                    existingFileId,
                    testFileEntity.getModeOfIntegration(),
                    stageToUpdate,
                    0L,
                    0L
            );
        });
        assertThat(thrown.getMessage()).contains("Stage progress not found for file: " + existingFileId + " and stage: " + stageToUpdate);
    }

     @Test
    void testUpdateStageProgressForInvalidStage() {
        // Arrange: Initialize stages first
        integrationStageService.initializeStages(testFileEntity);

        String invalidStage = "SOME_INVALID_STAGE_NAME";

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            integrationStageService.updateStageProgress(
                    testFileEntity.getId(),
                    testFileEntity.getModeOfIntegration(),
                    invalidStage,
                    0L,
                    0L
            );
        });
        assertThat(thrown.getMessage()).contains("Stage progress not found for file: " + testFileEntity.getId() + " and stage: " + invalidStage);
    }

} 