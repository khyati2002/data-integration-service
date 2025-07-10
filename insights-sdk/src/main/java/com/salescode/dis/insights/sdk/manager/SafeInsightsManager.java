package com.salescode.dis.insights.sdk.manager;


import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;

import java.util.Objects;
import java.util.Optional;

/**
 * A wrapper around InsightsManager that provides "safe" methods for API operations,
 * returning Optional<T> instead of throwing RestClientException on API call failures.
 * Errors are logged by the underlying InsightsManager or its components.
 */
@Slf4j
public class SafeInsightsManager {

    private final InsightsManager insightsManager;

    /**
     * Constructs a SafeInsightsManager.
     *
     * @param insightsManager The underlying InsightsManager instance to delegate calls to.
     *                        It is expected that this instance is fully initialized.
     */
    public SafeInsightsManager(InsightsManager insightsManager) {
        Objects.requireNonNull(insightsManager, "InsightsManager cannot be null for SafeInsightsManager");
        this.insightsManager = insightsManager;
        log.info("SafeInsightsManager initialized, wrapping InsightsManager for base URL: {}", insightsManager.getBaseURL());
    }

    public Optional<JobEntityResponseDto> createJob(String lob, JobEntityRequestDto jobRequest) {
        try {
            return Optional.ofNullable(insightsManager.createJob(lob, jobRequest));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: createJob operation failed for LOB '{}'. Returning Optional.empty().", lob);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: createJob operation called with invalid arguments for LOB '{}'. Returning Optional.empty().", lob, e);
            return Optional.empty();
        }
    }

    public Optional<FileEntityResponseDto> updateFileCount(String lob, String masterName, String fileId, Long totalCount) {
        try {
            return Optional.ofNullable(insightsManager.updateFileCount(lob, masterName,fileId,totalCount));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: createJob operation failed for LOB '{}'. Returning Optional.empty().", lob);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: createJob operation called with invalid arguments for LOB '{}'. Returning Optional.empty().", lob, e);
            return Optional.empty();
        }
    }

    public Optional<JobEntityResponseDto> updateJobStatus(String lob,String jobId, ProgressStatus status) {
        try {
            return Optional.ofNullable(insightsManager.updateJobStatus(lob, jobId, status));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: updateJobStatus operation failed for LOB '{}', Job ID '{}'. Returning Optional.empty().", lob, jobId);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: updateJobStatus operation called with invalid arguments for LOB '{}', Job ID '{}'. Returning Optional.empty().", lob, jobId, e);
            return Optional.empty();
        }
    }



    // --- Safe File Operations ---

    public Optional<FileEntityResponseDto> createFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest) {
        try {
            return Optional.ofNullable(insightsManager.createFile(lob, masterName, jobId, fileRequest));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: createFile operation failed for Job ID '{}', File ID '{}'. Returning Optional.empty().", jobId, fileRequest != null ? fileRequest.getFileId() : "N/A");
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: createFile operation called with invalid arguments for Job ID '{}'. Returning Optional.empty().", jobId, e);
            return Optional.empty();
        }
    }

    public Optional<FileEntityResponseDto> createFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest, String identifier) {
        try {
            return Optional.ofNullable(insightsManager.createFile(lob, masterName, jobId, fileRequest, identifier));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: createFile operation failed for Job ID '{}', File ID '{}'. Returning Optional.empty().", jobId, fileRequest != null ? fileRequest.getFileId() : "N/A");
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: createFile operation called with invalid arguments for Job ID '{}'. Returning Optional.empty().", jobId, e);
            return Optional.empty();
        }
    }



    public Optional<FileProgressResponse> updateFileProgress(String lob, String masterName, String fileId, FileProgressRequest progressPayload) {
        try {
            return Optional.ofNullable(insightsManager.updateFileProgress(lob, masterName, fileId, progressPayload));
        } catch (RestClientException e) {
            log.warn("SafeInsightsManager: updateFileProgress operation failed for File ID '{}'. Returning Optional.empty().", fileId);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("SafeInsightsManager: updateFileProgress operation called with invalid arguments for File ID '{}'. Returning Optional.empty().", fileId, e);
            return Optional.empty();
        }
    }

    public InsightsManager getUnderlyingInsightsManager() {
        return this.insightsManager;
    }
}