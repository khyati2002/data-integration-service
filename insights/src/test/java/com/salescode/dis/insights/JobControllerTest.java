package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.controller.JobController;
import com.salescode.dis.insights.dto.JobRequest;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.service.JobService;
import com.salescode.dis.insights.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void testCreateJob() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("env", env);
        body.put("lob", SecurityContextUtils.getLob());
        body.put("aggregationNames", successfulAggregationNames);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity("/api/{lob}/master/{master_name}/job",entity, String.class, Map.of("lob","master"));
    }

    @Test
    void testGetJob() {
        JobEntity job = new JobEntity();
        job.setId(1L);
        job.setLob("lob1");
        job.setMaster("master1");
        job.setStatus(JobStatus.PENDING);
        job.setTotalFileCount(5);
        job.setCompletedFiles(2);
        job.setFailedFiles(1);

        when(jobService.getJob(1L)).thenReturn(job);

        ResponseEntity<?> response = jobController.getJob("lob1", "master1", 1L);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("lob1"));
        verify(jobService, times(1)).getJob(1L);
    }

    @Test
    void testUpdateStatus() {
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("COMPLETED");

        JobEntity job = new JobEntity();
        job.setId(1L);
        job.setLob("lob1");
        job.setMaster("master1");
        job.setStatus(JobStatus.COMPLETED);
        job.setTotalFileCount(10);
        job.setCompletedFiles(10);
        job.setFailedFiles(0);

        when(jobService.updateStatus(1L, JobStatus.COMPLETED)).thenReturn(job);

        ResponseEntity<?> response = jobController.updateStatus("lob1", "master1", 1L, req);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("COMPLETED"));
        verify(jobService).updateStatus(1L, JobStatus.COMPLETED);
    }
}
