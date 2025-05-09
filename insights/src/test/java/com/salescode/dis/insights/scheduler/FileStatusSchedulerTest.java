package com.salescode.dis.insights.scheduler;

import com.salescode.dis.insights.dto.FileStatusRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileStatusSchedulerTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileStatusScheduler fileStatusScheduler;

    @Captor
    private ArgumentCaptor<FileStatusRequestDto> statusRequestCaptor;

    private FileEntity fileWithMatchingCounts;
    private FileEntity fileWithNonMatchingCounts;
    private Instant cutoffTime;

    @BeforeEach
    void setUp() {
        cutoffTime = Instant.now().minus(10, ChronoUnit.MINUTES);

        // Create a file with matching counts (should be marked as COMPLETED)
        fileWithMatchingCounts = new FileEntity();
        fileWithMatchingCounts.setId("file-matching");
        fileWithMatchingCounts.setMaster("master_name");
        fileWithMatchingCounts.setFileId("file_matching_123");
        fileWithMatchingCounts.setIsApiBased(true);
        fileWithMatchingCounts.setTotalCount(0); // Total count is always 0
        fileWithMatchingCounts.setConsumedSuccessCount(10);
        fileWithMatchingCounts.setConsumedFailCount(0);
        fileWithMatchingCounts.setPublishedSuccessCount(10);
        fileWithMatchingCounts.setPublishedFailCount(0);
        fileWithMatchingCounts.setConsumedStatus(FileStatus.PENDING);
        fileWithMatchingCounts.setPublishedStatus(FileStatus.PENDING);
        fileWithMatchingCounts.setJob(new JobEntity());

        // Create a file with non-matching counts (should be marked as FAILED)
        fileWithNonMatchingCounts = new FileEntity();
        fileWithNonMatchingCounts.setId("file-non-matching");
        fileWithNonMatchingCounts.setMaster("master_name");
        fileWithNonMatchingCounts.setFileId("file_non_matching_123");
        fileWithNonMatchingCounts.setIsApiBased(true);
        fileWithNonMatchingCounts.setTotalCount(0); // Total count is always 0
        fileWithNonMatchingCounts.setConsumedSuccessCount(5);
        fileWithNonMatchingCounts.setConsumedFailCount(2);
        fileWithNonMatchingCounts.setPublishedSuccessCount(4); // Different from consumed count
        fileWithNonMatchingCounts.setPublishedFailCount(1);
        fileWithNonMatchingCounts.setConsumedStatus(FileStatus.PENDING);
        fileWithNonMatchingCounts.setPublishedStatus(FileStatus.PENDING);
        fileWithNonMatchingCounts.setJob(new JobEntity());
    }

    @Test
    void testUpdateApiBasedFileStatus_WithMatchingCounts_ShouldMarkAsCompleted() {
        // Arrange
        when(fileRepository.findStaleApiBasedPendingFiles(any(Instant.class), any(Instant.class), eq(FileStatus.PENDING)))
                .thenReturn(List.of(fileWithMatchingCounts));

        // Act
        fileStatusScheduler.updateApiBasedFileStatus();

        // Assert
        verify(fileService).updateStatus(eq("file_matching_123"),eq("master_name"), statusRequestCaptor.capture());
        FileStatusRequestDto capturedRequest = statusRequestCaptor.getValue();
        assertEquals(FileStatus.COMPLETED, capturedRequest.getConsumedStatus());
        assertEquals(FileStatus.COMPLETED, capturedRequest.getPublishedStatus());
    }

    @Test
    void testUpdateApiBasedFileStatus_WithNonMatchingCounts_ShouldMarkAsFailed() {
        // Arrange
        when(fileRepository.findStaleApiBasedPendingFiles(any(Instant.class), any(Instant.class), eq(FileStatus.PENDING)))
                .thenReturn(List.of(fileWithNonMatchingCounts));

        // Act
        fileStatusScheduler.updateApiBasedFileStatus();

        // Assert
        verify(fileService).updateStatus(eq("file_non_matching_123"),eq("master_name"),statusRequestCaptor.capture());
        FileStatusRequestDto capturedRequest = statusRequestCaptor.getValue();
        assertEquals(FileStatus.FAILED, capturedRequest.getConsumedStatus());
        assertEquals(FileStatus.FAILED, capturedRequest.getPublishedStatus());
    }

    @Test
    void testUpdateApiBasedFileStatus_WithMultipleFiles_ShouldProcessAll() {
        // Arrange
        when(fileRepository.findStaleApiBasedPendingFiles(any(Instant.class), any(Instant.class), eq(FileStatus.PENDING)))
                .thenReturn(Arrays.asList(fileWithMatchingCounts, fileWithNonMatchingCounts));

        // Act
        fileStatusScheduler.updateApiBasedFileStatus();

        // Assert
        verify(fileService, times(2)).updateStatus(any(), any(),any());
    }

    @Test
    void testUpdateApiBasedFileStatus_WithNoFiles_ShouldNotCallFileService() {
        // Arrange
        when(fileRepository.findStaleApiBasedPendingFiles(any(Instant.class), any(Instant.class), eq(FileStatus.PENDING)))
                .thenReturn(List.of());

        // Act
        fileStatusScheduler.updateApiBasedFileStatus();

        // Assert
        verify(fileService, never()).updateStatus(any(),any(), any());
    }
}
