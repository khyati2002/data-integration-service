package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.enums.JobStatus;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Value
public class JobEntityResponseDtoWithFiles implements Serializable {
    String id;
    Instant creationTime;
    Instant lastModifiedTime;
    String lob;
    JsonNode extendedAttributes;
    Instant startTime;
    Instant endTime;
    String master;
    JobStatus status;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
    Integer completedFiles;
    Integer failedFiles;
    Integer publishedAverageThroughput;
    Integer consumedAverageThroughput;
    List<FileEntityResponseDto> files;
}