package com.salescode.dis.insights.sdk.manager;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.enums.JobStatus;
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
            String errorMessage = String.format("Error during job creation for LOB '%s'. URL: %s", lob, JOB_CREATE_URL);
            log.error(errorMessage, e);
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
            String errorMessage = String.format("Error during get job for LOB '%s', Job ID '%s'. URL: %s", lob, jobId, JOB_GET_BY_ID_URL);
            log.error(errorMessage, e);
            throw e;
        }
    }

    public JobEntityResponseDto updateJobById(String lob, String jobId, JobStatus status) {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        try {
            log.debug("Attempting to update status for job ID: {} to {} for LOB: {}", jobId, status, lob);
            Map<String, Object> uriVariables = new HashMap<>();
            uriVariables.put("lob", lob);
            uriVariables.put("jobId", jobId);
            uriVariables.put("status", status);

            ResponseEntity<JobEntityResponseDto> response = restTemplate.exchange(JOB_STATUS_UPDATE_URL, HttpMethod.PUT, entity, JobEntityResponseDto.class, uriVariables);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("Job status updated successfully for ID: {} to {}", jobId, status);
                return response.getBody();
            } else {
                String errorMessage = String.format("Job status update failed for LOB '%s', Job ID '%s', Status '%s'. Expected status %s but received %s. URL: %s", lob, jobId, status, HttpStatus.OK, response.getStatusCode(), JOB_STATUS_UPDATE_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            String errorMessage = String.format("Error during job status update for LOB '%s', Job ID '%s', Status '%s'. URL: %s", lob, jobId, status, JOB_STATUS_UPDATE_URL);
            log.error(errorMessage, e);
            throw e;
        }
    }
}
