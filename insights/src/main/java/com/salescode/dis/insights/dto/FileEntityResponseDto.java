package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.FileStatus;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link com.salescode.dis.insights.entity.FileEntity}
 */
@Value
public class FileEntityResponseDto implements Serializable {
    String id;
    String fileId;
    String master;
    Long max_processing_time;
    Long min_processing_time;
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
    Integer publisherThroughput;
    Integer consumerThroughput;
    FileStatus publishedStatus;
    FileStatus consumedStatus;

}