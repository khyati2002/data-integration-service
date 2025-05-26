package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.FileStatus;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.salescode.dis.insights.entity.FileEntity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
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
    Long totalCount;
    Long publishedSuccessCount;
    Long publishedFailCount;
    Long consumedSuccessCount;
    Long consumedFailCount;
    Long serverFailCount;
    Long logicalFailCount;
    Long retryCount;
    Long maxProcessingTimeMs;
    Long minProcessingTimeMs;
    BigDecimal publisherThroughput;
    BigDecimal consumerThroughput;
    FileStatus publishedStatus;
    FileStatus consumedStatus;

}