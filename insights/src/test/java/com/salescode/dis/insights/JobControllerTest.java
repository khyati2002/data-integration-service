package com.salescode.dis.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.controller.JobController;
import com.salescode.dis.insights.dto.JobRequest;
import com.salescode.dis.insights.dto.StatusUpdateRequest;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobControllerTest {

    private JobService jobService;
    private JobController jobController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jobService = Mockito.mock(JobService.class);
        jobController = new JobController(jobService);
    }

    @Test
    void testCreateJob() {
        JobRequest req = new JobRequest();
        JobEntity job = new JobEntity();
        job.setId(1L);

        when(jobService.createJob(any(JobRequest.class))).thenReturn(job);

        ResponseEntity<?> response = jobController.createJob("lob1", "master1", req);

        assertEquals(201, response.getStatusCodeValue());

        ObjectNode body = objectMapper.convertValue(response.getBody(), ObjectNode.class);
        assertEquals(1L, body.get("id").asLong());
        verify(jobService, times(1)).createJob(req);
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
