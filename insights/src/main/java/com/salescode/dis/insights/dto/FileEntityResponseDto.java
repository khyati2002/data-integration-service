package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.FileStatus;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.salescode.dis.insights.entity.FileEntity}
 */
@Value
public class FileEntityResponseDto implements Serializable {
    String id;
    String fileId;
    String master;
    Instant creationTime;
    Instant lastModifiedTime;
    String lob;
    JsonNode extendedAttributes;
    Instant startTime;
    Instant endTime;
    Integer totalCount;
    Integer publishedSuccessCount;
    Integer publishedFailCount;
    Integer consumedSuccessCount;
    Integer consumedFailCount;
    Integer serverFailCount;
    Integer logicalFailCount;
    Integer retryCount;
    Long maxProcessingTimeMs;
    Long minProcessingTimeMs;
    BigDecimal publisherThroughput;
    BigDecimal consumerThroughput;
    FileStatus publishedStatus;
    FileStatus consumedStatus;

}