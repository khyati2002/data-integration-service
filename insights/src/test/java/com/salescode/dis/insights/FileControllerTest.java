package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOB = "Retail";
    private static final String MASTER_NAME = "Master1";
    private static String createdJobId;
    private static String createdFileId;

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
        assertEquals(Integer.valueOf(10), response.getBody().getTotalFileCount());

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
        requestDto.setId("file-1");
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

    @Test
    @Order(4)
    void testUpdateFileProgress() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create progress update request
        FileUpdateRequestDto updateDto = new FileUpdateRequestDto();
        FileProgressRequest progressRequest = new FileProgressRequest();
        progressRequest.setConsumerSuccessCount(50);
        progressRequest.setConsumerFailCount(10);
        progressRequest.setPublishedSuccessCount(60);
        progressRequest.setPublishedFailCount(5);
        updateDto.setProgress(progressRequest);

        // Make request
        HttpEntity<FileUpdateRequestDto> entity = new HttpEntity<>(updateDto, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/update",
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
        assertEquals(50, response.getBody().getConsumedSuccessCount());
        assertEquals(10, response.getBody().getConsumedFailCount());
        assertEquals(60, response.getBody().getPublishedSuccessCount());
        assertEquals(5, response.getBody().getPublishedFailCount());
    }

    @Test
    @Order(5)
    void testUpdateFileStatus() {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create status update request
        FileUpdateRequestDto updateDto = new FileUpdateRequestDto();
        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
        statusRequest.setPublishedStatus(FileStatus.COMPLETED);
        updateDto.setStatus(statusRequest);

        // Make request
        HttpEntity<FileUpdateRequestDto> entity = new HttpEntity<>(updateDto, headers);
        ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/update",
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
        FileUpdateRequestDto updateDto = new FileUpdateRequestDto();

        // Make request
        HttpEntity<FileUpdateRequestDto> entity = new HttpEntity<>(updateDto, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}/update",
                HttpMethod.PUT,
                entity,
                String.class,
                LOB,
                MASTER_NAME,
                createdJobId,
                createdFileId
        );

        // Assertions for bad request (400)
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
}