package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.enums.ModeOfIntegration;
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
    String publisherJobUri;
    String consumerJobUri;
    ModeOfIntegration modeOfIntegration;
    ProgressStatus status;
}