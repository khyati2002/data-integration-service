package com.salescode.dis.insights.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileStatusControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    private String jobId;
    private String fileId;
    private final String lob = "test-lob";
    private final String masterName = "test-master";
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create a job first
        JobEntityRequestDto jobRequest = createSampleJobRequest();
        ResponseEntity<JobEntityResponseDto> jobResponse = restTemplate.postForEntity(
                "/api/{lob}/master/{master_name}/job",
                new HttpEntity<>(jobRequest, headers),
                JobEntityResponseDto.class,
                lob, masterName
        );

        assertEquals(HttpStatus.CREATED, jobResponse.getStatusCode());
        assertNotNull(jobResponse.getBody());
        jobId = jobResponse.getBody().getId();

        // Create a file
        FileEntityRequestDto fileRequest = new FileEntityRequestDto();
        fileRequest.setId("file-" + UUID.randomUUID());
        fileRequest.setTotalCount(100);

        ObjectNode extendedAttrs = objectMapper.createObjectNode();
        extendedAttrs.put("source", "test");
        extendedAttrs.put("priority", "high");
        fileRequest.setExtendedAttributes(extendedAttrs);

        ResponseEntity<FileEntityResponseDto> fileResponse = restTemplate.postForEntity(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit",
                new HttpEntity<>(fileRequest, headers),
                FileEntityResponseDto.class,
                lob, masterName, jobId
        );

        assertEquals(HttpStatus.CREATED, fileResponse.getStatusCode());
        assertNotNull(fileResponse.getBody());
        fileId = fileResponse.getBody().getId();
    }

    @Test
    void testUpdateFileStatus() {
        // Create status update request
        FileStatusRequestDto statusRequest = new FileStatusRequestDto();
        statusRequest.setConsumedStatus(FileStatus.COMPLETED);
        statusRequest.setPublishedStatus(FileStatus.COMPLETED);

        // Send status update request
        ResponseEntity<FileEntityResponseDto> updateResponse = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest, headers),
                FileEntityResponseDto.class,
                lob, masterName, fileId
        );

        // Verify the response
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertNotNull(updateResponse.getBody());
        assertEquals(FileStatus.COMPLETED, updateResponse.getBody().getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, updateResponse.getBody().getPublishedStatus());

        // Verify the file status was updated in the database
        FileEntity file = fileService.get(fileId);
        assertEquals(FileStatus.COMPLETED, file.getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, file.getPublishedStatus());

        // Verify the file status can be retrieved via the API
        ResponseEntity<FileEntityResponseDto> getResponse = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FileEntityResponseDto.class,
                lob, masterName, jobId, fileId
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(FileStatus.COMPLETED, getResponse.getBody().getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, getResponse.getBody().getPublishedStatus());
    }

    private JobEntityRequestDto createSampleJobRequest() {
        ObjectNode extendedAttrs = objectMapper.createObjectNode();
        extendedAttrs.put("source", "test");
        extendedAttrs.put("priority", "high");

        return new JobEntityRequestDto(
                extendedAttrs,
                "http://publisher/job/" + UUID.randomUUID(),
                "http://consumer/job/" + UUID.randomUUID()
        );
    }
}
