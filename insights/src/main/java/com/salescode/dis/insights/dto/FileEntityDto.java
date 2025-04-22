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
public class FileEntityDto implements Serializable {
    String id;
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
    Integer publisherThroughput;
    Integer consumerThroughput;
    FileStatus status;
}