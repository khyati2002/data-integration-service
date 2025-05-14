package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres", "dev", "debug", "kafka", "test"})
class FileControllerWithSameFileIdDifferentMastersTest {

    private static final String LOB = "Retail";
    private static final String MASTER_NAME = "Master1";
    private static final String MASTER_NAME2 = "Master2";
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
        requestDto.setTotalCount(100);

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
        assertEquals(extendedAttrs, response.getBody().getExtendedAttributes());

        // Count related assertions
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedFailCount());

        assertEquals(Integer.valueOf(0), response.getBody().getConsumedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getConsumedFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getLogicalFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getServerFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getRetryCount());

        assertNull(response.getBody()
                .getMaxProcessingTimeMs(), "Max processing time should be null at start, will be set by progress");
        assertNull(response.getBody()
                .getMinProcessingTimeMs(), "Min processing time should be null at start, will be set by progress");

        assertNull(response.getBody()
                .getConsumerThroughput(), "Consumer throughput is updated when file is marked success or failed");
        assertNull(response.getBody()
                .getPublisherThroughput(), "Publisher throughput is updated when file is marked success or failed");

        assertEquals(FileStatus.PENDING, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.PENDING, response.getBody().getPublishedStatus());

        // Time-related assertions
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
        assertEquals(Integer.valueOf(1), jobResponse.getBody().getTotalFileCount());
    }

    @Test
    @Order(2)
    void testRegisterFile2() throws Exception {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body
        FileEntityRequestDto requestDto = new FileEntityRequestDto();

        requestDto.setFileId(FILE_ID);
        requestDto.setTotalCount(100);

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
                MASTER_NAME2,
                createdJobId
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getFileId());

        assertEquals(FILE_ID,response.getBody().getFileId());

        assertEquals(MASTER_NAME2, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(100, response.getBody().getTotalCount());
        assertEquals(extendedAttrs, response.getBody().getExtendedAttributes());

        // Count related assertions
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedFailCount());

        assertEquals(Integer.valueOf(0), response.getBody().getConsumedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getConsumedFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getLogicalFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getServerFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getRetryCount());

        assertNull(response.getBody()
                .getMaxProcessingTimeMs(), "Max processing time should be null at start, will be set by progress");
        assertNull(response.getBody()
                .getMinProcessingTimeMs(), "Min processing time should be null at start, will be set by progress");

        assertNull(response.getBody()
                .getConsumerThroughput(), "Consumer throughput is updated when file is marked success or failed");
        assertNull(response.getBody()
                .getPublisherThroughput(), "Publisher throughput is updated when file is marked success or failed");

        assertEquals(FileStatus.PENDING, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.PENDING, response.getBody().getPublishedStatus());

        // Time-related assertions
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
        assertEquals(Integer.valueOf(2), jobResponse.getBody().getTotalFileCount());
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

        // Count related assertions
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedFailCount());

        assertEquals(Integer.valueOf(0), response.getBody().getConsumedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getConsumedFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getLogicalFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getServerFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getRetryCount());

        assertNull(response.getBody()
                .getMaxProcessingTimeMs(), "Max processing time should be null at start, will be set by progress");
        assertNull(response.getBody()
                .getMinProcessingTimeMs(), "Min processing time should be null at start, will be set by progress");

        assertNull(response.getBody()
                .getConsumerThroughput(), "Consumer throughput is updated when file is marked success or failed");
        assertNull(response.getBody()
                .getPublisherThroughput(), "Publisher throughput is updated when file is marked success or failed");

        assertEquals(FileStatus.PENDING, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.PENDING, response.getBody().getPublishedStatus());

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null for a newly created job
        assertNull(response.getBody().getEndTime(), "End time should be null for a new file");
    }

    @Test
    @Order(3)
    void testGetFile2() {
        // Make request using the ID from the create test
        ResponseEntity<FileEntityResponseDto> response = restTemplate.getForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME2,
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
        assertEquals(MASTER_NAME2, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(100, response.getBody().getTotalCount());

        // Count related assertions
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getPublishedFailCount());

        assertEquals(Integer.valueOf(0), response.getBody().getConsumedSuccessCount());
        assertEquals(Integer.valueOf(0), response.getBody().getConsumedFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getLogicalFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getServerFailCount());
        assertEquals(Integer.valueOf(0), response.getBody().getRetryCount());

        assertNull(response.getBody()
                .getMaxProcessingTimeMs(), "Max processing time should be null at start, will be set by progress");
        assertNull(response.getBody()
                .getMinProcessingTimeMs(), "Min processing time should be null at start, will be set by progress");

        assertNull(response.getBody()
                .getConsumerThroughput(), "Consumer throughput is updated when file is marked success or failed");
        assertNull(response.getBody()
                .getPublisherThroughput(), "Publisher throughput is updated when file is marked success or failed");

        assertEquals(FileStatus.PENDING, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.PENDING, response.getBody().getPublishedStatus());

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

        // Create progress update request
        FileProgressRequest progressRequest = new FileProgressRequest();

        // Set consumer metrics
        FileProgressRequest.ConsumerMetrics consumer = new FileProgressRequest.ConsumerMetrics();
        consumer.setSuccessCount(50);
        consumer.setServerFailCount(5);
        consumer.setLogicalFailCount(5);
        consumer.setRetryCount(5);
        progressRequest.setConsumer(consumer);

        // Set publisher metrics
        FileProgressRequest.PublisherMetrics publisher = new FileProgressRequest.PublisherMetrics();
        publisher.setSuccessCount(60);
        publisher.setFailCount(5);
        progressRequest.setPublisher(publisher);

        // Make request
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

        System.out.println("Response body: " + response.getBody());
        System.out.println("Response status: " + response.getStatusCode());

        Awaitility.await()
                .atMost(1000000, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    FileEntity file = fileService.get(createdFileId, MASTER_NAME);
                    System.out.println(file);
                    return file.getConsumedSuccessCount() != 0;
                });

        System.out.println("Master name is :" + MASTER_NAME);
        FileEntity file = fileService.get(createdFileId, MASTER_NAME);
        System.out.println("File is " + file);
        assertEquals(file.getConsumedSuccessCount(), consumer.getSuccessCount());
        assertEquals(file.getConsumedFailCount(), consumer.getServerFailCount() + consumer.getLogicalFailCount());
        assertEquals(file.getLogicalFailCount(), consumer.getLogicalFailCount());
        assertEquals(file.getRetryCount(), consumer.getRetryCount());
        assertEquals(file.getPublishedSuccessCount(), publisher.getSuccessCount());
        assertEquals(file.getPublishedFailCount(), publisher.getFailCount());

        // Assertions
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @SneakyThrows
    @Test
    @Order(4)
    void testUpdateFileProgress2() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create progress update request
        FileProgressRequest progressRequest = new FileProgressRequest();

        // Set consumer metrics
        FileProgressRequest.ConsumerMetrics consumer = new FileProgressRequest.ConsumerMetrics();
        consumer.setSuccessCount(50);
        consumer.setServerFailCount(5);
        consumer.setLogicalFailCount(5);
        consumer.setRetryCount(5);
        progressRequest.setConsumer(consumer);

        // Set publisher metrics
        FileProgressRequest.PublisherMetrics publisher = new FileProgressRequest.PublisherMetrics();
        publisher.setSuccessCount(60);
        publisher.setFailCount(5);
        progressRequest.setPublisher(publisher);

        // Make request
        HttpEntity<FileProgressRequest> entity = new HttpEntity<>(progressRequest, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/progress",
                HttpMethod.PUT,
                entity,
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME2,
                createdFileId
        );

        System.out.println("Response body: " + response.getBody());
        System.out.println("Response status: " + response.getStatusCode());

        Awaitility.await()
                .atMost(1000000, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    FileEntity file = fileService.get(createdFileId, MASTER_NAME2);
                    System.out.println(file);
                    return file.getConsumedSuccessCount() != 0;
                });

        System.out.println("Master name is :" + MASTER_NAME2);
        FileEntity file = fileService.get(createdFileId, MASTER_NAME2);
        System.out.println("File is " + file);
        assertEquals(file.getConsumedSuccessCount(), consumer.getSuccessCount());
        assertEquals(file.getConsumedFailCount(), consumer.getServerFailCount() + consumer.getLogicalFailCount());
        assertEquals(file.getLogicalFailCount(), consumer.getLogicalFailCount());
        assertEquals(file.getRetryCount(), consumer.getRetryCount());
        assertEquals(file.getPublishedSuccessCount(), publisher.getSuccessCount());
        assertEquals(file.getPublishedFailCount(), publisher.getFailCount());

        // Assertions
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @Order(5)
    void testUpdateFileStatus() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create status update request
        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
        statusRequest.setPublishedStatus(FileStatus.COMPLETED);

        // Make request
        HttpEntity<FileStatusRequestDto> entity = new HttpEntity<>(statusRequest, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
                HttpMethod.PUT,
                entity,
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(FileStatus.COMPLETED, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, response.getBody().getPublishedStatus());

        JobEntity job = jobRepository.findById(createdJobId).orElseGet(null);
        assertNotNull(job);
        assertEquals(2, job.getTotalFileCount());
        assertEquals(1, job.getCompletedFiles());
        assertEquals(0, job.getFailedFiles());
        assertEquals(JobStatus.PENDING, job.getStatus());
    }

    @Test
    @Order(5)
    void testUpdateFileStatus2() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create status update request
        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
        statusRequest.setPublishedStatus(FileStatus.COMPLETED);

        // Make request
        HttpEntity<FileStatusRequestDto> entity = new HttpEntity<>(statusRequest, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
                HttpMethod.PUT,
                entity,
                FileEntityResponseDto.class,
                LOB,
                MASTER_NAME2,
                createdJobId,
                createdFileId
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(FileStatus.COMPLETED, response.getBody().getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, response.getBody().getPublishedStatus());

        JobEntity job = jobRepository.findById(createdJobId).orElseGet(null);
        assertNotNull(job);
        assertEquals(2, job.getTotalFileCount());
        assertEquals(2, job.getCompletedFiles());
        assertEquals(0, job.getFailedFiles());
        assertEquals(JobStatus.COMPLETED, job.getStatus());
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
        assertEquals(2,response.getBody().getTotalElements());
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
        FileStatusRequestDto updateDto = new FileStatusRequestDto();

        // Make request
        HttpEntity<FileStatusRequestDto> entity = new HttpEntity<>(updateDto, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/status",
                HttpMethod.PUT,
                entity,
                String.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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