package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres", "dev", "debug"})
class FileControllerTest {

    private static final String LOB = "Retail";
    private static final String MASTER_NAME = "Master1";
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
                "/api/{lob}/master/{master_name}/job",
                entity,
                JobEntityResponseDto.class,
                LOB,
                MASTER_NAME
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(MASTER_NAME, response.getBody().getMaster());
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
        requestDto.setId(UUID.randomUUID().toString());
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
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(100, response.getBody().getTotalCount());

        // Store the ID for later tests
        createdFileId = response.getBody().getId();

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
        assertEquals(createdFileId, response.getBody().getId());
        assertEquals(LOB, response.getBody().getLob());
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

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    FileEntity file = fileService.get(createdFileId);
                    return file.getConsumedSuccessCount() != null;
                });

        assertEquals(fileService.get(createdFileId).getConsumedSuccessCount(), consumer.getSuccessCount());
        assertEquals(fileService.get(createdFileId).getConsumedFailCount(), consumer.getServerFailCount());
        assertEquals(fileService.get(createdFileId).getLogicalFailCount(), consumer.getLogicalFailCount());
        assertEquals(fileService.get(createdFileId).getPublishedSuccessCount(), publisher.getSuccessCount());
        assertEquals(fileService.get(createdFileId).getPublishedFailCount(), publisher.getFailCount());

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
    }

    @Test
    @Order(6)
    void testListFilesByJob() {
        // Build URI with query parameters and pagination
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/{lob}/master/{master_name}/job/{jobId}/unit");

        // Make request
        ResponseEntity<List<FileEntityResponseDto>> response = restTemplate.exchange(
                builder.buildAndExpand(LOB, MASTER_NAME, createdJobId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FileEntityResponseDto>>() {}
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0);
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