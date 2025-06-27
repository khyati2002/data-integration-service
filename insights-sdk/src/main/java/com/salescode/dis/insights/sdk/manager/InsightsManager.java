package com.salescode.dis.insights.sdk.manager;
import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.sdk.InsightsEnv;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.web.client.RestClientException; // Import RestClientException
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InsightsManager {

    @Getter
    private final JobManager jobManager;

    @Getter
    private final FileManager fileManager;

    @Getter
    private final String baseURL;
    private final Map<String, FileEntityResponseDto> fileEntityResponseDtoMap = new ConcurrentHashMap<>();
    private final Map<String, FileProgressResponse> updateRequestResponseDtoMap = new ConcurrentHashMap<>();
    public Optional<JobEntityResponseDto> getJobEntityResponseDto() {
        return Optional.ofNullable(jobEntityResponseDto);
    }

    @Setter
    private JobEntityResponseDto jobEntityResponseDto;


    InsightsManager(RestTemplate restTemplate, InsightsEnv env) {
        Objects.requireNonNull(restTemplate, "RestTemplate cannot be null");
        Objects.requireNonNull(env, "InsightsEnv cannot be null");
        this.baseURL = env.getInsightsUrl();
        this.jobManager = new JobManager(restTemplate, baseURL);
        this.fileManager = new FileManager(restTemplate, baseURL);
        log.info("InsightsManager initialized with base URL: {}", baseURL);
    }

    // --- Job Operations ---

    public JobEntityResponseDto createJob(String lob, JobEntityRequestDto jobRequest) {
        Objects.requireNonNull(lob, "LOB cannot be null for createJob");
        Objects.requireNonNull(jobRequest, "JobEntityRequestDto cannot be null for createJob");

        try {
            log.debug("Calling JobManager to create job for LOB: {}", lob);
            JobEntityResponseDto createdJob = this.jobManager.createJob(lob,jobRequest);
            // Assuming jobManager.createJob throws an exception if 'createdJob' or its ID is null/invalid
            this.setJobEntityResponseDto(createdJob);
            log.debug("Job creation successful, updated internal state.");
            return createdJob;
        } catch (RestClientException e) {
            log.error("Failed to create job via InsightsManager for LOB: {}, ExceptionMsg: {}", lob, e.getMessage());
            throw e; // Re-throw the exception from the manager
        }
    }


    // --- File Operations ---

    public FileEntityResponseDto createFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest) {
        Objects.requireNonNull(lob, "LOB cannot be null for createFile");
        Objects.requireNonNull(masterName, "MasterName cannot be null for createFile");
        Objects.requireNonNull(jobId, "JobId cannot be null for createFile");
        Objects.requireNonNull(fileRequest, "FileEntityRequestDto cannot be null for createFile");
        Objects.requireNonNull(fileRequest.getFileId(), "FileId within FileEntityRequestDto cannot be null for createFile");

        try {
            log.debug("Calling FileManager to create file with ID: {} for Job ID: {}", fileRequest.getFileId(), jobId);
            FileEntityResponseDto createdFile = this.fileManager.createFile(lob, masterName, jobId, fileRequest);
            addFileEntityResponse(createdFile.getFileId(), createdFile);
            log.debug("File creation successful, added to internal map.");
            return createdFile;
        } catch (RestClientException e) {
            log.error("Failed to create file via InsightsManager for File ID: {}, ExceptionMsg: {}", fileRequest.getFileId(), e.getMessage());
            throw e; // Re-throw the exception from the manager
        }
    }

    public FileEntityResponseDto createFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest, String identifier) {
        Objects.requireNonNull(lob, "LOB cannot be null for createFile");
        Objects.requireNonNull(masterName, "MasterName cannot be null for createFile");
        Objects.requireNonNull(jobId, "JobId cannot be null for createFile");
        Objects.requireNonNull(fileRequest, "FileEntityRequestDto cannot be null for createFile");

        try {
            log.debug("Calling FileManager to create file with ID: {} for Job ID: {}", fileRequest.getFileId(), jobId);
            FileEntityResponseDto createdFile = this.fileManager.createFile(lob, masterName, jobId, fileRequest);
            addFileEntityResponse(identifier, createdFile);
            log.debug("File creation successful, added to internal map.");
            return createdFile;
        } catch (RestClientException e) {
            log.error("Failed to create file via InsightsManager for File ID: {}, ExceptionMsg: {}", fileRequest.getFileId(), e.getMessage());
            throw e; // Re-throw the exception from the manager
        }
    }

    public FileEntityResponseDto updateFileCount(String lob, String masterName, String fileId, Long totalCount){
        Objects.requireNonNull(lob, "LOB cannot be null for createFile");
        Objects.requireNonNull(masterName, "MasterName cannot be null for createFile");
        Objects.requireNonNull(fileId, "FileId cannot be null for createFile");
        try {
            log.debug("Calling FileManager to update total with ID: {} ", fileId);
            FileEntityResponseDto createdFile = this.fileManager.updateCount(lob, masterName, fileId, totalCount);
            log.debug("Count updated successfully");
            return createdFile;
        } catch (RestClientException e) {
            log.error("Failed to create file via InsightsManager for File ID: {}, ExceptionMsg: {}", fileId, e.getMessage());
            throw e; // Re-throw the exception from the manager
        }

    }



    public FileProgressResponse updateFileProgress(String lob, String masterName, String fileId, FileProgressRequest progressPayload) {
        Objects.requireNonNull(lob, "LOB cannot be null for updateFileProgress");
        Objects.requireNonNull(masterName, "MasterName cannot be null for updateFileProgress");
        Objects.requireNonNull(fileId, "FileId cannot be null for updateFileProgress");
        Objects.requireNonNull(progressPayload, "FileProgressRequest cannot be null for updateFileProgress");

        try {
            log.debug("Calling FileManager to update progress for file ID: {}", fileId);
            fileId = Optional.ofNullable(getFileEntityResponse(fileId)).map(FileEntityResponseDto::getFileId).orElse(fileId);
            FileProgressResponse updateResponse = this.fileManager.updateFileProgress(
                    lob, masterName, fileId, progressPayload);
            // Storing the response if it has a request ID, for potential tracking.
            if (updateResponse != null && updateResponse.getRequestId() != null) {
               addUpdateRequestResponse(updateResponse.getRequestId(), updateResponse);
                log.debug("File progress update successful, added update response to internal map.");
            } else {
                log.debug("File progress update successful, but no request ID in response.");
            }
            return updateResponse;
        } catch (RestClientException e) {
            log.error("Failed to update file progress via InsightsManager for File ID: {}, ExceptionMsg: {}", fileId, e.getMessage());
            throw e; // Re-throw the exception from the manager
        }
    }



    // --- Helper methods ---

    public void addUpdateRequestResponse(String updateRequestId, FileProgressResponse updateRequestResponseDto) {
        Objects.requireNonNull(updateRequestId, "updateRequestId cannot be null");
        Objects.requireNonNull(updateRequestResponseDto, "updateRequestResponseDto cannot be null");
        updateRequestResponseDtoMap.put(updateRequestId, updateRequestResponseDto);
        log.trace("Added UpdateRequestResponseDto for ID: {}", updateRequestId);
    }

    public FileProgressResponse getUpdateRequestResponse(String updateRequestId) {
        Objects.requireNonNull(updateRequestId, "updateRequestId cannot be null");
        log.trace("Retrieving UpdateRequestResponseDto for ID: {}", updateRequestId);
        return updateRequestResponseDtoMap.get(updateRequestId);
    }

    public void addFileEntityResponse(String fileId, FileEntityResponseDto fileEntityResponseDto) {
        Objects.requireNonNull(fileId, "fileId cannot be null");
        Objects.requireNonNull(fileEntityResponseDto, "fileEntityResponseDto cannot be null");
        fileEntityResponseDtoMap.put(fileId, fileEntityResponseDto);
        log.trace("Added FileEntityResponseDto for ID: {}", fileId);
    }

    public FileEntityResponseDto getFileEntityResponse(String fileId) {
        Objects.requireNonNull(fileId, "fileId cannot be null");
        log.trace("Retrieving FileEntityResponseDto for ID: {}", fileId);
        return fileEntityResponseDtoMap.get(fileId);
    }
}