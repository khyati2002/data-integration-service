package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.FileEntityRequestDto;
import com.salescode.dis.insights.dto.FileEntityResponseDto;
import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.dto.StageDefinition;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.IntegrationStageProgress;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.IntegrationMode;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.service.FileIntegrationProcessor;
import com.salescode.dis.insights.service.IntegrationStageService;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres", "dev", "debug", "kafka", "test"})
public class FileIntegrationFullLifecycleApiTest {

    private static final String LOB = "RetailFull";
    private static final String MASTER_NAME = "MasterFull";
    private static String createdJobId;
    private static String createdFileId;
    private static Long createdFileDbId; // To get the actual DB ID for service calls

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private IntegrationStageService integrationStageService;
    @Autowired
    private FileIntegrationProcessor fileIntegrationProcessor;

    @BeforeAll
    void setUp() {
        // 1. Create a Job via API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JobEntityRequestDto jobRequestDto = createSampleJobRequest();
        ResponseEntity<JobEntityResponseDto> jobResponse = restTemplate.postForEntity(
                "/api/{lob}/job",
                new HttpEntity<>(jobRequestDto, headers),
                JobEntityResponseDto.class,
                LOB
        );
        assertEquals(HttpStatus.CREATED, jobResponse.getStatusCode());
        assertNotNull(jobResponse.getBody());
        createdJobId = jobResponse.getBody().getId();
        System.out.println("Created Job ID in BeforeAll: " + createdJobId);

        // 2. Register a File via API
        FileEntityRequestDto fileRequestDto = new FileEntityRequestDto();
        fileRequestDto.setFileId(UUID.randomUUID().toString()); // Generate a unique file ID
        fileRequestDto.setTotalCount(200L);
        fileRequestDto.setExtendedAttributes(objectMapper.createObjectNode().put("source", "test_full_api"));

        ResponseEntity<FileEntityResponseDto> fileResponse = restTemplate.postForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit",
                new HttpEntity<>(fileRequestDto, headers),
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId
        );
        assertEquals(HttpStatus.CREATED, fileResponse.getStatusCode());
        assertNotNull(fileResponse.getBody());
        createdFileId = fileResponse.getBody().getFileId();
        createdFileDbId = fileResponse.getBody().getId();
        System.out.println("Created File ID in BeforeAll: " + createdFileId + ", DB ID: " + createdFileDbId);

        // Ensure the FileEntity is fully loaded for service calls
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> fileRepository.findByFileIdAndMaster(createdFileId, MASTER_NAME).isPresent());
    }

    @Test
    @Order(1)
    @SneakyThrows
    void testFullFileIntegrationLifecycle() {
        // Get the FileEntity for service calls
        FileEntity fileEntity = fileRepository.findById(createdFileDbId)
                .orElseThrow(() -> new AssertionError("FileEntity not found after setup."));

        // 3. Initialize Stages (Service Call)
        integrationStageService.initializeStages(fileEntity);

        List<IntegrationStageProgress> initialProgressRecords = fileRepository.findById(createdFileDbId)
                .map(FileEntity::getStageProgress)
                .orElseThrow(() -> new AssertionError("Stage progress not found after initialization."));

        assertThat(initialProgressRecords).hasSize(fileIntegrationProcessor.getStagesForMode().size());
        initialProgressRecords.forEach(stage -> {
            assertThat(stage.getSuccessCount()).isZero();
            assertThat(stage.getFailCount()).isZero();
            assertThat(stage.getIsCompleted()).isFalse();
            assertThat(stage.getDescription()).isNotBlank();
            assertThat(stage.getActionToBeTaken()).isNotBlank();
            assertThat(stage.getHowToCheckIssue()).isNotBlank();
        });

        // 4. Simulate Progression Through Each Stage (Service Calls)
        List<StageDefinition> fileStages = fileIntegrationProcessor.getStagesForMode();

        for (int i = 0; i < fileStages.size(); i++) {
            StageDefinition currentStageDef = fileStages.get(i);
            long successCount = (i + 1) * 10L; // Simulate increasing success
            long failCount = (i + 1) * 2L;     // Simulate increasing failures

            integrationStageService.updateStageProgress(
                    fileEntity.getId(),
                    fileEntity.getModeOfIntegration(),
                    currentStageDef.getName(),
                    successCount,
                    failCount
            );

            // Verify the current stage's update in DB
            Optional<IntegrationStageProgress> updatedStageOptional = stageProgressRepository.findByFileIdAndStage(fileEntity.getId(), currentStageDef.getName());
            assertThat(updatedStageOptional).isPresent();
            IntegrationStageProgress updatedStage = updatedStageOptional.get();

            assertThat(updatedStage.getSuccessCount()).isEqualTo(successCount);
            assertThat(updatedStage.getFailCount()).isEqualTo(failCount);
            assertThat(updatedStage.getIsCompleted()).isTrue();

            // Ensure metadata is NOT changed by update (it's set during initialization)
            assertThat(updatedStage.getDescription()).isEqualTo(currentStageDef.getDescription());
            assertThat(updatedStage.getActionToBeTaken()).isEqualTo(currentStageDef.getActionToBeTaken());
            assertThat(updatedStage.getHowToCheckIssue()).isEqualTo(currentStageDef.getHowToCheckIssue());
        }

        // 5. Update Overall File Progress (Publisher/Consumer counts) via API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        FileProgressRequest progressRequest = new FileProgressRequest();
        progressRequest.setConsumer(new FileProgressRequest.ConsumerMetrics(180L, 5L, 5L, 2L));
        progressRequest.setPublisher(new FileProgressRequest.PublisherMetrics(190L, 10L));

        ResponseEntity<FileEntityResponseDto> progressUpdateResponse = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/progress",
                HttpMethod.PUT,
                new HttpEntity<>(progressRequest, headers),
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdFileId
        );
        assertEquals(HttpStatus.ACCEPTED, progressUpdateResponse.getStatusCode());

        // Wait for asynchronous updates to settle, if any
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> { // Use fileRepository to get the latest state from DB
                    FileEntity currentFile = fileRepository.findByFileIdAndMaster(createdFileId, MASTER_NAME).orElse(null);
                    return currentFile != null && currentFile.getConsumedSuccessCount().equals(progressRequest.getConsumer().getSuccessCount());
                });

        // 6. Update Overall File Status to COMPLETED via API
        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
        statusRequest.setPublishedStatus(FileStatus.COMPLETED);

        ResponseEntity<FileEntityResponseDto> statusUpdateResponse = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest, headers),
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );
        assertEquals(HttpStatus.OK, statusUpdateResponse.getStatusCode());

        // 7. Final Verification of FileEntity state after completion (via API GET)
        ResponseEntity<FileEntityResponseDto> finalFileGetResponse = restTemplate.getForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );
        assertEquals(HttpStatus.OK, finalFileGetResponse.getStatusCode());
        FileEntityResponseDto finalFileDto = finalFileGetResponse.getBody();

        assertNotNull(finalFileDto);
        assertEquals(FileStatus.COMPLETED, finalFileDto.getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, finalFileDto.getPublishedStatus());
        assertNotNull(finalFileDto.getEndTime());
        // Verify aggregated counts from API response
        assertThat(finalFileDto.getConsumedSuccessCount()).isEqualTo(progressRequest.getConsumer().getSuccessCount());
        assertThat(finalFileDto.getPublishedSuccessCount()).isEqualTo(progressRequest.getPublisher().getSuccessCount());

        // Verify Job status also changed to COMPLETED if it's the only file
        JobEntity finalJob = jobRepository.findById(createdJobId).orElseThrow(() -> new AssertionError("Job not found after completion."));
        assertThat(finalJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(finalJob.getCompletedFiles()).isEqualTo(1);
        assertThat(finalJob.getFailedFiles()).isEqualTo(0);

        // Verify all stage progress records directly from DB
        List<IntegrationStageProgress> finalProgressRecords = stageProgressRepository.findByFileId(createdFileDbId);
        assertThat(finalProgressRecords).hasSize(fileIntegrationProcessor.getStagesForMode().size());
        finalProgressRecords.forEach(stage -> {
            assertThat(stage.getIsCompleted()).isTrue(); // All stages should be completed
            assertThat(stage.getSuccessCount()).isNotZero(); // Based on simulation logic
            assertThat(stage.getFailCount()).isNotZero();   // Based on simulation logic
            assertThat(stage.getDescription()).isNotBlank();
            assertThat(stage.getActionToBeTaken()).isNotBlank();
            assertThat(stage.getHowToCheckIssue()).isNotBlank();
        });
    }

    private JobEntityRequestDto createSampleJobRequest() {
        ObjectNode extendedAttrs = objectMapper.createObjectNode();
        extendedAttrs.put("source", "test_full_api");
        extendedAttrs.put("priority", "high");

        return new JobEntityRequestDto(
                extendedAttrs,
                "http://publisher/job/789",
                "http://consumer/job/987"
        );
    }

    @AfterAll
    void cleanUpAllCreatedEntities() {
        if (createdFileDbId != null) {
            fileRepository.deleteById(createdFileDbId); // Deletes File and cascaded stage progress
        }
        if (createdJobId != null) {
            jobRepository.deleteById(createdJobId);
        }
    }
} 