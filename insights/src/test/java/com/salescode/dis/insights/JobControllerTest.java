package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.TimeAwareEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.repository.JobRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres","dev","debug"})
class JobControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOB = "Retail";
    private static final String MASTER = "Master1";
    private static String createdJobId;

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
                MASTER
        );

        // Assertions
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(MASTER, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(JobStatus.PENDING, response.getBody().getStatus());
        assertEquals("http://publisher/job/123", response.getBody().getPublisherJobUri());
        assertEquals("http://consumer/job/456", response.getBody().getConsumerJobUri());
        assertEquals(Integer.valueOf(0), response.getBody().getTotalFileCount());
        assertEquals(Integer.valueOf(0), response.getBody().getCompletedFiles());
        assertEquals(Integer.valueOf(0), response.getBody().getFailedFiles());

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null for a newly created job
        assertNull(response.getBody().getEndTime(), "End time should be null for a new job");

        // Store the ID for later tests
        createdJobId = response.getBody().getId();

        // Verify location header
        assertTrue(Objects.requireNonNull(response.getHeaders().getLocation()).toString().contains(createdJobId));
    }

    @Test
    @Order(2)
    void testGetJob() {
        // Make request using the ID from the create test
        ResponseEntity<JobEntityResponseDto> response = restTemplate.getForEntity(
                "/api/{lob}/master/{master_name}/job/{id}",
                JobEntityResponseDto.class,
                LOB,
                MASTER,
                createdJobId
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdJobId, response.getBody().getId());
        assertEquals(MASTER, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals(JobStatus.PENDING, response.getBody().getStatus());
        assertEquals("http://publisher/job/123", response.getBody().getPublisherJobUri());
        assertEquals("http://consumer/job/456", response.getBody().getConsumerJobUri());
        assertEquals(Integer.valueOf(0), response.getBody().getTotalFileCount());
        assertEquals(Integer.valueOf(0), response.getBody().getCompletedFiles());
        assertEquals(Integer.valueOf(0), response.getBody().getFailedFiles());

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null for a newly created job
        assertNull(response.getBody().getEndTime(), "End time should be null for a new job");
    }

    @Test
    @Order(3)
    void testUpdateStatus() {
        // Make request to update status to RUNNING
        ResponseEntity<JobEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{id}/status/{status}",
                HttpMethod.PUT,
                null,
                JobEntityResponseDto.class,
                LOB,
                MASTER,
                createdJobId,
                "PENDING"
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(JobStatus.PENDING, response.getBody().getStatus());
        assertEquals(createdJobId, response.getBody().getId());
        assertEquals(MASTER, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals("http://publisher/job/123", response.getBody().getPublisherJobUri());
        assertEquals("http://consumer/job/456", response.getBody().getConsumerJobUri());
        assertEquals(Integer.valueOf(0), response.getBody().getTotalFileCount());
        assertEquals(Integer.valueOf(0), response.getBody().getCompletedFiles());
        assertEquals(Integer.valueOf(0), response.getBody().getFailedFiles());

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        // End time should be null as the job is not completed
        assertNull(response.getBody().getEndTime(), "End time should be null for a job that is not completed");
    }

    @Test
    @Order(4)
    void testGetJobs() {
        // Build URI with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/{lob}/master/jobs");

        // Make request
        ResponseEntity<List<JobEntityResponseDto>> response = restTemplate.exchange(
                builder.buildAndExpand(LOB).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<JobEntityResponseDto>>() {}
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        boolean foundCreatedJob = response.getBody().stream()
                .anyMatch(job -> job.getId().equals(createdJobId));
        assertTrue(foundCreatedJob, "Should find the job we created earlier");
    }

    @Test
    @Order(5)
    void testGetJobsByMaster() {
        // Build URI with query parameters and pagination
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/{lob}/master/{master_name}/jobs");

        // Make request
        ResponseEntity<List<JobEntityResponseDto>> response = restTemplate.exchange(
                builder.buildAndExpand(LOB, MASTER).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<JobEntityResponseDto>>() {}
        );


        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
        boolean foundCreatedJob = response.getBody().stream()
                .anyMatch(job -> job.getId().equals(createdJobId));
        assertTrue(foundCreatedJob, "Should find the job we created earlier");
    }


    @Test
    @Order(7)
    void testGetJob_NotFound() {
        // Make request with a non-existent job ID
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{id}",
                HttpMethod.GET,
                null,
                String.class,
                LOB,
                MASTER,
                "nonexistent-job-id-123456789"
        );

        // Assertions for not found (404) or other error response
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(8)
    void testUpdateStatus_InvalidStatus() {
        // Make request with an invalid status value
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{id}/status/{status}",
                HttpMethod.PUT,
                null,
                String.class,
                LOB,
                MASTER,
                createdJobId,
                "INVALID_STATUS"
        );

        // Assertions for bad request (400) or other error response
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(9)
    void testCompleteJobLifecycle() {
        // Update to COMPLETED status
        ResponseEntity<JobEntityResponseDto> response = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{id}/status/{status}",
                HttpMethod.PUT,
                null,
                JobEntityResponseDto.class,
                LOB,
                MASTER,
                createdJobId,
                "COMPLETED"
        );

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(JobStatus.COMPLETED, response.getBody().getStatus());
        assertEquals(createdJobId, response.getBody().getId());
        assertEquals(MASTER, response.getBody().getMaster());
        assertEquals(LOB, response.getBody().getLob());
        assertEquals("http://publisher/job/123", response.getBody().getPublisherJobUri());
        assertEquals("http://consumer/job/456", response.getBody().getConsumerJobUri());
        assertEquals(Integer.valueOf(0), response.getBody().getTotalFileCount());
        assertEquals(Integer.valueOf(0), response.getBody().getCompletedFiles());
        assertEquals(Integer.valueOf(0), response.getBody().getFailedFiles());

        // Time-related assertions
        assertNotNull(response.getBody().getCreationTime(), "Creation time should not be null");
        assertNotNull(response.getBody().getLastModifiedTime(), "Last modified time should not be null");
        assertNotNull(response.getBody().getStartTime(), "Start time should not be null");
        assertNotNull(response.getBody().getEndTime(), "End time should be set when job is completed");

        // Verify that endTime is after startTime
        assertTrue(response.getBody().getEndTime().isAfter(response.getBody().getStartTime()), 
                "End time should be after start time");
    }

    // Helper method to create sample request data
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
    static void cleanupAll(@Autowired JobRepository staticJobRepository) {
        if (createdJobId != null) {
            System.out.println("Cleaning up job with ID: " + createdJobId);
            staticJobRepository.deleteById(createdJobId);
            createdJobId = null;
        }
    }
}