package com.salescode.dis.insights.sdk.manager;


import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JobManager {

    private final String JOB_CREATE_URL;
    private final String JOB_GET_BY_ID_URL;
    private final String JOB_STATUS_UPDATE_URL;

    RestTemplate restTemplate;

    private JobManager(String baseURL) {
        JOB_CREATE_URL = baseURL + "/api/{lob}/job";
        JOB_GET_BY_ID_URL = baseURL + "/api/{lob}/job/{jobId}";
        JOB_STATUS_UPDATE_URL = baseURL + "/api/{lob}/job/{jobId}/status/{status}";
    }

    JobManager(RestTemplate restTemplate, String baseURL) {
        this(baseURL);
        this.restTemplate = restTemplate;
    }

    private static HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public JobEntityResponseDto createJob(String lob, JobEntityRequestDto jobRequest) {
        HttpEntity<JobEntityRequestDto> entity = new HttpEntity<>(jobRequest, getHttpHeaders());
        try {
            log.debug("Attempting to create job for LOB: {}", lob);
            ResponseEntity<JobEntityResponseDto> response = restTemplate.postForEntity(JOB_CREATE_URL, entity, JobEntityResponseDto.class, lob);
            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                if (response.getBody() != null) {
                    log.info("Job created successfully for LOB: {} with ID: {}", lob, response.getBody().getId());
                }
                return response.getBody();
            } else {
                String errorMessage = String.format("Job creation failed for LOB '%s'. Expected status %s but received %s. URL: %s", lob, HttpStatus.CREATED, response.getStatusCode(), JOB_CREATE_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during job creation for LOB '{}'. URL: {}, ExceptionMsg {}", lob, JOB_CREATE_URL, e.getMessage());
            throw e;
        }
    }

    public JobEntityResponseDto getJobById(String lob, String jobId) {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        try {
            log.debug("Attempting to get job with ID: {} for LOB: {}", jobId, lob);
            ResponseEntity<JobEntityResponseDto> response = restTemplate.exchange(JOB_GET_BY_ID_URL, HttpMethod.GET, entity, JobEntityResponseDto.class, lob, jobId);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("Job retrieved successfully for ID: {}", jobId);
                return response.getBody();
            } else {
                String errorMessage = String.format("Get job failed for LOB '%s', Job ID '%s'. Expected status %s but received %s. URL: %s", lob, jobId, HttpStatus.OK, response.getStatusCode(), JOB_GET_BY_ID_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during get job for LOB '{}', Job ID '{}'. URL: {}, ExceptionMsg {}", lob, jobId, JOB_GET_BY_ID_URL, e.getMessage());
            throw e;
        }
    }

    public JobEntityResponseDto updateJobStatus(String lob, String jobId, ProgressStatus status) {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        try {
            log.debug("Attempting to update status of job with ID: {} to {} for LOB: {}", jobId, status, lob);

            ResponseEntity<JobEntityResponseDto> response = restTemplate.exchange(
                    JOB_STATUS_UPDATE_URL,
                    HttpMethod.PUT,
                    entity,
                    JobEntityResponseDto.class,
                    lob,
                    jobId,
                    status.name()
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Job status updated successfully for ID: {}, new status: {}", jobId, status);
                return response.getBody();
            } else {
                String errorMessage = String.format(
                        "Failed to update job status for LOB '%s', Job ID '%s'. Expected status %s but got %s. URL: %s",
                        lob, jobId, HttpStatus.OK, response.getStatusCode(), JOB_STATUS_UPDATE_URL
                );
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }

        } catch (RestClientException e) {
            log.error("Error updating job status for LOB '{}', Job ID '{}'. URL: {}, Exception: {}",
                    lob, jobId, JOB_STATUS_UPDATE_URL, e.getMessage());
            throw e;
        }
    }

}
