package com.salescode.dis.insights;

import com.salescode.dis.insights.dto.JobRequest;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    private JobEntity testJobEntity;
    private JobRequest testJobRequest;

    @BeforeEach
    void setUp() {
        // Set up test data
        testJobEntity = new JobEntity();
        testJobEntity.setId(1L);
        testJobEntity.setLob("retail");
        testJobEntity.setMaster("customer");
        testJobEntity.setStatus(JobStatus.PENDING);
        testJobEntity.setTotalFileCount(10);
        testJobEntity.setCompletedFiles(5);
        testJobEntity.setFailedFiles(1);

        testJobRequest = new JobRequest();
        // Set properties based on your JobRequest class
        // For example:
        // testJobRequest.setLob("retail");
        // testJobRequest.setMaster("customer");
    }

    @Test
    void createJob_ShouldSaveAndReturnJobEntity() {
        // Given
        when(jobRepository.save(any(JobEntity.class))).thenReturn(testJobEntity);

        // When
        JobEntity result = jobService.createJob(testJobRequest);

        // Then
        assertNotNull(result);
        assertEquals(testJobEntity.getId(), result.getId());
        assertEquals(testJobEntity.getLob(), result.getLob());
        assertEquals(testJobEntity.getMaster(), result.getMaster());
        assertEquals(JobStatus.PENDING, result.getStatus());
        verify(jobRepository, times(1)).save(any(JobEntity.class));
    }

    @Test
    void getJob_WithExistingId_ShouldReturnJobEntity() {
        // Given
        Long jobId = 1L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJobEntity));

        // When
        JobEntity result = jobService.getJob(jobId);

        // Then
        assertNotNull(result);
        assertEquals(testJobEntity.getId(), result.getId());
        assertEquals(testJobEntity.getLob(), result.getLob());
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    void getJob_WithNonExistingId_ShouldThrowException() {
        // Given
        Long jobId = 999L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            jobService.getJob(jobId);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    void updateStatus_ShouldUpdateAndReturnJobEntity() {
        // Given
        Long jobId = 1L;
        JobStatus newStatus = JobStatus.COMPLETED;
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJobEntity));
        
        JobEntity updatedEntity = new JobEntity();
        updatedEntity.setId(testJobEntity.getId());
        updatedEntity.setLob(testJobEntity.getLob());
        updatedEntity.setMaster(testJobEntity.getMaster());
        updatedEntity.setStatus(newStatus);
        updatedEntity.setTotalFileCount(testJobEntity.getTotalFileCount());
        updatedEntity.setCompletedFiles(testJobEntity.getCompletedFiles());
        updatedEntity.setFailedFiles(testJobEntity.getFailedFiles());
        
        when(jobRepository.save(any(JobEntity.class))).thenReturn(updatedEntity);

        // When
        JobEntity result = jobService.updateStatus(jobId, newStatus);

        // Then
        assertNotNull(result);
        assertEquals(jobId, result.getId());
        assertEquals(newStatus, result.getStatus());
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, times(1)).save(any(JobEntity.class));
    }
    
    @Test
    void updateStatus_WithNonExistingId_ShouldThrowException() {
        // Given
        Long jobId = 999L;
        JobStatus newStatus = JobStatus.COMPLETED;
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            jobService.updateStatus(jobId, newStatus);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
        verify(jobRepository, times(1)).findById(jobId);
        verify(jobRepository, never()).save(any(JobEntity.class));
    }
}