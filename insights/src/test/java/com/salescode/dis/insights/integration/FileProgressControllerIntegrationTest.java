package com.salescode.dis.insights.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.dto.*;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.service.FileService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.awaitility.Awaitility;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"postgres", "dev", "debug", "kafka", "test"})
public class FileProgressControllerIntegrationTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create a job first
        JobEntityRequestDto jobRequest = createSampleJobRequest();
        ResponseEntity<JobEntityResponseDto> jobResponse = restTemplate.postForEntity(
                "/api/{lob}/job",
                new HttpEntity<>(jobRequest, headers),
                JobEntityResponseDto.class,
                lob
        );

        assertEquals(HttpStatus.CREATED, jobResponse.getStatusCode());
        assertNotNull(jobResponse.getBody());
        jobId = jobResponse.getBody().getId();

        // Create a file
        FileEntityRequestDto fileRequest = new FileEntityRequestDto();
        fileRequest.setFileId("file-" + UUID.randomUUID());
        fileRequest.setTotalCount(100L);
        
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
        fileId = fileResponse.getBody().getFileId();
    }

    @Test
    void testMultipleProgressUpdatesAreAggregated() {
        // First progress update
        FileProgressRequest progressRequest1 = new FileProgressRequest();
        progressRequest1.setProcessingTimeMs(1000L);
        
        FileProgressRequest.ConsumerMetrics consumer1 = new FileProgressRequest.ConsumerMetrics();
        consumer1.setSuccessCount(10L);
        consumer1.setServerFailCount(2L);
        consumer1.setLogicalFailCount(1L);
        consumer1.setRetryCount(5L);
        progressRequest1.setConsumer(consumer1);
        
        FileProgressRequest.PublisherMetrics publisher1 = new FileProgressRequest.PublisherMetrics();
        publisher1.setSuccessCount(15L);
        publisher1.setFailCount(3L);
        progressRequest1.setPublisher(publisher1);

        // Send first progress update
        ResponseEntity<UpdateRequestResponseDto> updateResponse1 = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/progress",
                HttpMethod.PUT,
                new HttpEntity<>(progressRequest1, headers),
                UpdateRequestResponseDto.class,
                lob, masterName, fileId
        );

        assertEquals(HttpStatus.ACCEPTED, updateResponse1.getStatusCode());
        
        // Wait for the first update to be processed
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    FileEntity file = fileService.get(fileId,masterName);
                    return file.getConsumedSuccessCount() != null && file.getConsumedSuccessCount() == 10;
                });

        // Second progress update with different values
        FileProgressRequest progressRequest2 = new FileProgressRequest();
        progressRequest2.setProcessingTimeMs(200L);
        
        FileProgressRequest.ConsumerMetrics consumer2 = new FileProgressRequest.ConsumerMetrics();
        consumer2.setSuccessCount(20L);
        consumer2.setServerFailCount(3L);
        consumer2.setLogicalFailCount(2L);
        consumer2.setRetryCount(5L);
        progressRequest2.setConsumer(consumer2);
        
        FileProgressRequest.PublisherMetrics publisher2 = new FileProgressRequest.PublisherMetrics();
        publisher2.setSuccessCount(25L);
        publisher2.setFailCount(5L);
        progressRequest2.setPublisher(publisher2);

        // Send second progress update
        ResponseEntity<UpdateRequestResponseDto> updateResponse2 = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/unit/{fileId}/progress",
                HttpMethod.PUT,
                new HttpEntity<>(progressRequest2, headers),
                UpdateRequestResponseDto.class,
                lob, masterName, fileId
        );

        assertEquals(HttpStatus.ACCEPTED, updateResponse2.getStatusCode());

        // Wait for both updates to be processed and aggregated
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    FileEntity file = fileService.get(fileId,masterName);
                    return file.getConsumedSuccessCount() != null && 
                           file.getConsumedSuccessCount() == 30 && // 10 + 20
                           file.getServerFailCount() != null && 
                           file.getServerFailCount() == 5 && // 2 + 3
                           file.getLogicalFailCount() != null && 
                           file.getLogicalFailCount() == 3 && // 1 + 2
                           file.getPublishedSuccessCount() != null && 
                           file.getPublishedSuccessCount() == 40 && // 15 + 25
                           file.getPublishedFailCount() != null && 
                           file.getPublishedFailCount() == 8 && // 3 + 5
                           file.getRetryCount() == 10; // 5 + 5
                });

        // Verify the aggregated metrics through the API
        ResponseEntity<FileEntityResponseDto> getResponse = restTemplate.exchange(
                "/api/{lob}/master/{master_name}/job/{jobId}/unit/{fileId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FileEntityResponseDto.class,
                lob, masterName, jobId, fileId
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        
        // Verify all metrics were correctly aggregated
        assertEquals(30, getResponse.getBody().getConsumedSuccessCount()); // 10 + 20
        assertEquals(5, getResponse.getBody().getServerFailCount()); // 2 + 3
        assertEquals(3, getResponse.getBody().getLogicalFailCount()); // 1 + 2
        assertEquals(40, getResponse.getBody().getPublishedSuccessCount()); // 15 + 25
        assertEquals(8, getResponse.getBody().getPublishedFailCount()); // 3 + 5
        assertEquals(10, getResponse.getBody().getRetryCount()); // 5 + 5

        assertEquals(200, getResponse.getBody().getMinProcessingTimeMs());
        assertEquals(1000, getResponse.getBody().getMaxProcessingTimeMs());
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