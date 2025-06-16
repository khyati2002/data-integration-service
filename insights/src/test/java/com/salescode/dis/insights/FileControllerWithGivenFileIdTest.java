package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.mapper.pagination.RestPageImpl;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.service.FileService;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.salescode.dis.insights.enums.ProgressStage.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres", "dev", "info", "kafka", "test"})
@Transactional
class FileControllerWithGivenFileIdTest {

    private static final String LOB = "Retail";
    private static final String MASTER_NAME = "Master1";
    public static final String FILE_ID = UUID.randomUUID().toString();
    private static String createdJobId;
    private static String createdFileId;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private JobRepository jobRepository;


    @Test
    @Order(1)
    void testCreateJob() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body with actual fields from JobEntityRequestDto
        JobEntityRequestDto requestDto = createSampleJobRequest();

        // Make request
        HttpEntity<JobEntityRequestDto> entity = new HttpEntity<>(requestDto, headers);
        ResponseEntity<JobEntityResponseDto> response = restTemplate.postForEntity(
                "/api/{lob}/job",
                entity,
                JobEntityResponseDto.class,
                LOB
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        //     assertEquals(MASTER_NAME, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(JobStatus.PENDING, response.getBody().getStatus());
        assertEquals("http://publisher/job/123", response.getBody().getPublisherJobUri());
        assertEquals("http://consumer/job/456", response.getBody().getConsumerJobUri());

        // Store the ID for later tests
        createdJobId = response.getBody().getId();
        System.out.println("Created Job ID: " + createdJobId);

    }

    @Test
    @Order(2)
    void testRegisterFile() throws Exception {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body
        FileEntityRequestDto requestDto = new FileEntityRequestDto();
        requestDto.setFileId(FILE_ID);
        requestDto.setTotalCount(100L);
        requestDto.setModeOfIntegration(ModeOfIntegration.CK_FILE);

        // Create sample extended attributes JSON
        ObjectNode extendedAttrs = objectMapper.createObjectNode();
        extendedAttrs.put("source", "test");
        extendedAttrs.put("priority", "high");
        requestDto.setExtendedAttributes(extendedAttrs);

        // Make request
        HttpEntity<FileEntityRequestDto> entity = new HttpEntity<>(requestDto, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.postForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit",
                entity,
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getFileId());
        assertEquals(FILE_ID,response.getBody().getFileId());
        assertEquals(MASTER_NAME, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(100, response.getBody().getTotalCount());
        assertEquals(ModeOfIntegration.CK_FILE, response.getBody().getModeOfIntegration());
        assertFalse(response.getBody().getStageMetrics().isEmpty());
        assertEquals(extendedAttrs, response.getBody().getExtendedAttributes());

        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null for a newly created job
        assertNull(response.getBody().getEndTime(), "End time should be null for a new file");

        // Store the ID for later tests
        createdFileId = response.getBody().getFileId();


        // Make request using the ID from the create test
        ResponseEntity<JobEntityResponseDto> jobResponse = restTemplate.getForEntity(
                "/api/{lob}/job/{id}",
                JobEntityResponseDto.class,
                LOB,
                createdJobId
        );

        // Assertions
        assertEquals(HttpStatus.OK, jobResponse.getStatusCode());
        assertNotNull(jobResponse.getBody());
        assertEquals(createdJobId, jobResponse.getBody().getId());
        assertEquals(LOB, jobResponse.getBody().getLob());
        assertEquals(JobStatus.PENDING, jobResponse.getBody().getStatus());
//        assertEquals(Integer.valueOf(1), jobResponse.getBody().getTotalFileCount());
    }

    @Test
    @Order(3)
    void testGetFile() {
        // Make request using the ID from the create test
        ResponseEntity<FileEntityResponseDto> response = restTemplate.getForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdFileId, response.getBody().getFileId());
        assertEquals(LOB, response.getBody().getLob());

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getFileId());
        assertEquals(MASTER_NAME, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(100, response.getBody().getTotalCount());
        assertEquals(ModeOfIntegration.CK_FILE, response.getBody().getModeOfIntegration());
        assertFalse(response.getBody().getStageMetrics().isEmpty());

//        assertNull(response.getBody()
//                .getMaxProcessingTimeMs(), "Max processing time should be null at start, will be set by progress");
//        assertNull(response.getBody()
//                .getMinProcessingTimeMs(), "Min processing time should be null at start, will be set by progress");

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null for a newly created job
        assertNull(response.getBody().getEndTime(), "End time should be null for a new file");
    }


    @SneakyThrows
    @Test
    @Order(4)
    void testUpdateFileProgress() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Test progress update for READ stage
        updateAndAssertFileProgressForStage(headers, READ, 10L, 1L, 5, 50);

        // Test progress update for PUBLISH stage
        updateAndAssertFileProgressForStage(headers, PUBLISH, 60L, 5L, 10, 100);

        // Test progress update for QUEUE stage
        updateAndAssertFileProgressForStage(headers, QUEUE, 20L, 2L, 15, 70);

        // Test progress update for PROCESS stage
        updateAndAssertFileProgressForStage(headers, PROCESS, 50L, 10L, 20, 120);

        // Test progress update for SAVE stage
        updateAndAssertFileProgressForStage(headers, SAVE, 90L, 0L, 25, 150);
    }

    @SneakyThrows
    protected void updateAndAssertFileProgressForStage(HttpHeaders headers, ProgressStage stageName, Long successCount,
                                                       Long failureCount, int minProcessingTime, int maxProcessingTime) {
        FileProgressRequest progressRequest = new FileProgressRequest();
        progressRequest.setStageName(stageName);
        progressRequest.setSuccessCount(successCount);
        progressRequest.setFailureCount(failureCount);
        progressRequest.setMinProcessingTimeMs(minProcessingTime);
        progressRequest.setMaxProcessingTimeMs(maxProcessingTime);

        HttpEntity<FileProgressRequest> entity = new HttpEntity<>(progressRequest, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/progress",
                HttpMethod.PUT,
                entity,
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdFileId
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());

        // Await for the metrics to be updated in the database
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .until(() -> {
                    ResponseEntity<FileEntityResponseDto> response1 = restTemplate.getForEntity(
                            "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                            FileEntityResponseDto.class,
                            LOB,
                            MASTER_NAME,
                            createdJobId,
                            createdFileId
                    );

                    return Objects.requireNonNull(response1.getBody()).getStageMetrics().stream()
                            .anyMatch(metrics -> stageName.equals(metrics.getStageType()) && metrics.getSuccessCount() == successCount);
                });

        ResponseEntity<FileEntityResponseDto> response1 = restTemplate.getForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );
        FileEntityResponseDto file = response1.getBody();
        Objects.requireNonNull(file).getStageMetrics().stream()
                .filter(metrics -> stageName.equals(metrics.getStageType()))
                .findFirst()
                .ifPresentOrElse(metrics -> {
                    assertEquals(successCount, metrics.getSuccessCount());
                    assertEquals(failureCount, metrics.getFailureCount());
                    assertEquals(minProcessingTime, metrics.getMinProcessingTimeMs());
                    assertEquals(maxProcessingTime, metrics.getMaxProcessingTimeMs());
                }, () -> fail(stageName + " stage metrics not found"));
    }

    @Test
    @Order(5)
    void testUpdateFileStatus() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create status update request
//        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
//        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
//        statusRequest.setPublishedStatus(FileStatus.COMPLETED);
//
//        // Make request
//        HttpEntity<FileStatusRequestDto> entity = new HttpEntity<>(statusRequest, headers);
//        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
//                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
//                HttpMethod.PUT,
//                entity,
//                FileEntityResponseDto.class,
//                LOB,
//                MASTER_NAME,
//                createdJobId,
//                createdFileId
//        );

        // Assertions
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
        // The commented out assertions below are no longer valid as consumedStatus and publishedStatus are not directly on FileEntityResponseDto
        // assertEquals(FileStatus.COMPLETED, response.getBody().getConsumedStatus());
        // assertEquals(FileStatus.COMPLETED, response.getBody().getPublishedStatus());

        JobEntity job = jobRepository.findById(createdJobId).orElse(null);
        assertNotNull(job);
//        assertEquals(1, job.getCompletedFiles());
//        assertEquals(0, job.getFailedFiles());
//        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    @Order(6)
    void testListFilesByJob() {
        // Make request
        ResponseEntity<RestPageImpl<FileEntityResponseDto>> response = restTemplate.exchange(
                "/api/{lob}/job/{jobId}/unit",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                LOB, createdJobId
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1,response.getBody().getTotalElements());
        assertFalse(response.getBody().getContent().isEmpty());
    }

    @Test
    @Order(7)
    void testGetFile_NotFound() {
        // Make request with a non-existent file ID
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                HttpMethod.GET,
                null,
                String.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                "nonexistent-file-id-123456789"
        );

        // Assertions for not found (404) or other error response
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(8)
    void testUpdateFileWithInvalidRequest() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create an empty update request with neither progress nor status
//        FileStatusRequestDto updateDto = new FileStatusRequestDto();
//
//        // Make request
//        HttpEntity<FileStatusRequestDto> entity = new HttpEntity<>(updateDto, headers);
//        ResponseEntity<String> response = restTemplate.exchange(
//                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
//                HttpMethod.PUT,
//                entity,
//                String.class,
//                LOB,
//                MASTER_NAME,
//                createdJobId,
//                createdFileId
//        );

//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    private JobEntityRequestDto createSampleJobRequest() {
        // Create sample extended attributes JSON
        ObjectNode extendedAttrs = objectMapper.createObjectNode();
        extendedAttrs.put("source", "test");
        extendedAttrs.put("priority", "high");

        JobEntityRequestDto dto = new JobEntityRequestDto(
                extendedAttrs,
                "http://publisher/job/123",
                "http://consumer/job/456"
        );

        return dto;
    }

    @AfterAll
    void cleanUpAllCreatedEntities() {
        if (createdFileId != null) {
            try {
                fileRepository.deleteById(createdFileId);
            } catch (Exception ignored) {
            }
        }
        if (createdJobId != null) {
            try {
                jobRepository.deleteById(createdJobId);
            } catch (Exception ignored) {
            }
        }
    }

}