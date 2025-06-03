package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.JobStatus;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class JobEntityResponseDto implements Serializable {
    String id;
    Instant creationTime;
    Instant lastModifiedTime;
    String lob;
    JsonNode extendedAttributes;
    Instant startTime;
    Instant endTime;
    JobStatus status;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
    Integer completedFiles;
    Integer failedFiles;
}