package com.salescode.dis.insights.sdk.manager;

import com.salescode.dis.insights.dto.file.FileEntityRequestDto;
import com.salescode.dis.insights.dto.file.FileEntityResponseDto;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.dto.file.progress.FileProgressResponse;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
public class FileManager {

    private final String FILE_CREATE_URL;
    private final String FILE_GET_URL;
    private final String FILE_STATUS_UPDATE_URL;
    private final String FILE_COUNT_UPDATE_URL;
    private final String FILE_PROGRESS_UPDATE_URL;
    private final String FILE_UPDATE_URL;

    RestTemplate restTemplate;

    private FileManager(String baseURL) {
        FILE_CREATE_URL = baseURL + "/api/{lob}/master/{masterName}/job/{jobId}/unit";
        FILE_GET_URL = baseURL + "/api/{lob}/master/{masterName}/job/{jobId}/unit/{fileId}";
        FILE_STATUS_UPDATE_URL = baseURL + "/api/{lob}/master/{masterName}/job/{jobId}/unit/{fileId}/status";
        FILE_COUNT_UPDATE_URL = baseURL + "/api/{lob}/master/{masterName}/unit/{fileId}/total-count";
        FILE_PROGRESS_UPDATE_URL = baseURL + "/api/{lob}/master/{masterName}/unit/{fileId}/progress";
        FILE_UPDATE_URL = baseURL + "/api/{lob}/master/{master_name}/job/{jobId}";

    }

    FileManager(RestTemplate restTemplate, String baseURL) {
        this(baseURL);
        this.restTemplate = restTemplate;
    }

    private static HttpHeaders getHttpHeaders(String authorizationToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authorizationToken);
        return headers;
    }

    public FileEntityResponseDto createFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest, String authorizationToken) {
        Objects.requireNonNull(fileRequest, "FileEntityRequestDto cannot be null");

        HttpEntity<FileEntityRequestDto> entity = new HttpEntity<>(fileRequest, getHttpHeaders(authorizationToken));
        try {
            log.debug("Attempting to create file with ID: {} for LOB: {}, Master: {}, Job ID: {}", fileRequest.getFileId(), lob, masterName, jobId);
            ResponseEntity<FileEntityResponseDto> response = restTemplate.postForEntity(FILE_CREATE_URL, entity, FileEntityResponseDto.class, lob, masterName, jobId);
            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                if (response.getBody() != null) {
                    log.info("File created successfully with ID: {}", response.getBody().getFileId());
                }
                return response.getBody();
            } else {
                String errorMessage = String.format("File creation failed for LOB '%s', Master '%s', Job ID '%s', File ID '%s'. Expected status %s but received %s. URL: %s", lob, masterName, jobId, fileRequest.getFileId(), HttpStatus.CREATED, response.getStatusCode(), FILE_CREATE_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during file creation for LOB '{}', Master '{}', Job ID '{}', File ID '{}'. URL: {}, ExceptionMsg {}", lob, masterName, jobId, fileRequest.getFileId(), FILE_CREATE_URL, e.getMessage());
            throw e;
        }
    }

    public FileEntityResponseDto updateFile(String lob, String masterName, String jobId, FileEntityRequestDto fileRequest, String authorizationToken) {
        Objects.requireNonNull(fileRequest, "FileEntityRequestDto cannot be null");
        HttpEntity<FileEntityRequestDto> entity = new HttpEntity<>(fileRequest, getHttpHeaders(authorizationToken));
        try {
            log.debug("Attempting to update file with ID: {} for LOB: {}, Master: {}, Job ID: {}", fileRequest.getFileId(), lob, masterName, jobId);
            ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(FILE_UPDATE_URL, HttpMethod.PUT, entity, FileEntityResponseDto.class, lob, masterName, jobId);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("File updated successfully with ID: {}", fileRequest.getFileId());
                return response.getBody();
            } else {
                String errorMessage = String.format("Update file count failed for LOB '%s', Master '%s', File ID '%s',. Expected status %s but received %s. URL: %s",
                        lob, masterName, fileRequest.getFileId(), HttpStatus.OK, response.getStatusCode(), FILE_UPDATE_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during file creation for LOB '{}', Master '{}', Job ID '{}', File ID '{}'. URL: {}, ExceptionMsg {}", lob, masterName, jobId, fileRequest.getFileId(), FILE_CREATE_URL, e.getMessage());
            throw e;
        }
    }

    public FileEntityResponseDto getFile(String lob, String masterName, String jobId, String fileId, String authorizationToken) {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders(authorizationToken));
        try {
            log.debug("Attempting to get file with ID: {} for LOB: {}, Master: {}, Job ID: {}", fileId, lob, masterName, jobId);
            ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(FILE_GET_URL, HttpMethod.GET, entity, FileEntityResponseDto.class, lob, masterName, jobId, fileId);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("File retrieved successfully with ID: {}", fileId);
                return response.getBody();
            } else {
                String errorMessage = String.format("Get file failed for LOB '%s', Master '%s', Job ID '%s', File ID '%s'. Expected status %s but received %s. URL: %s", lob, masterName, jobId, fileId, HttpStatus.OK, response.getStatusCode(), FILE_GET_URL);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during get file for LOB '{}', Master '{}', Job ID '{}', File ID '{}'. URL: {}, ExceptionMsg {}", lob, masterName, jobId, fileId, FILE_GET_URL, e.getMessage());
            throw e;
        }
    }

    public FileEntityResponseDto updateCount(String lob, String masterName, String fileId, Long totalCount, String authorizationToken) {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders(authorizationToken));
        try {
            log.debug("Attempting to update count file with ID: {} for LOB: {}, Master: {}, totalCount: {}", fileId, lob, masterName, totalCount);
            String urlWithParams = FILE_COUNT_UPDATE_URL + "?totalCount=" + totalCount;
            ResponseEntity<FileEntityResponseDto> response = restTemplate.exchange(urlWithParams, HttpMethod.PUT, entity, FileEntityResponseDto.class, lob, masterName, fileId);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("File count updated successfully with ID: {}, totalCount: {}", fileId, totalCount);
                return response.getBody();
            } else {
                String errorMessage = String.format("Update file count failed for LOB '%s', Master '%s', File ID '%s', totalCount '%s'. Expected status %s but received %s. URL: %s",
                        lob, masterName, fileId, totalCount, HttpStatus.OK, response.getStatusCode(), urlWithParams);
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        } catch (RestClientException e) {
            log.error("Error during update file count for LOB '{}', Master '{}', File ID '{}', totalCount '{}'. URL: {}, ExceptionMsg {}",
                    lob, masterName, fileId, totalCount, FILE_COUNT_UPDATE_URL, e.getMessage());
            throw e;
        }
    }

    public FileProgressResponse updateFileProgress(String lob, String masterName, String fileId, FileProgressRequest progressPayload, String authorizationToken) {
        Objects.requireNonNull(progressPayload, "FileProgressRequest cannot be null");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FileProgressRequest> entity = new HttpEntity<>(progressPayload,headers);
        try {
            log.debug("Attempting to update progress for file ID: {} for LOB: {}, Master: {}", fileId, lob, masterName);
            ResponseEntity<FileProgressResponse> response = restTemplate.exchange(FILE_PROGRESS_UPDATE_URL, HttpMethod.PUT, entity, FileProgressResponse.class, lob, masterName, fileId);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("File progress updated successfully for ID: {}", fileId);
                return response.getBody();
            } else {
                String errorMessage = String.format("File progress update for LOB '%s', Master '%s', File ID '%s' returned non-2xx status: %s. URL: %s. Response: %s", lob, masterName, fileId, response.getStatusCode(), FILE_PROGRESS_UPDATE_URL, response.getBody());
                log.warn(errorMessage);
                throw new RestClientException(errorMessage);
            }
        }
        catch (RestClientException e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpException = (HttpClientErrorException) e;
                if (httpException.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.error("File not found (404) for LOB '{}', Master '{}', File ID '{}'", lob, masterName, fileId);
                    throw new ResourceNotFoundException("File not found: " + fileId);
                }
            }
            log.error("Failed to update file progress via InsightsManager for File ID: {}, ExceptionMsg: {}", fileId, e.getMessage());
            throw e;
        }

    }
}