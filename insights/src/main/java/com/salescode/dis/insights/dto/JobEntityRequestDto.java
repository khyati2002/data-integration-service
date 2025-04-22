package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;

import com.salescode.dis.insights.enums.JobStatus;
import lombok.Value;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Value
public class JobEntityRequestDto implements Serializable {
    JsonNode extendedAttributes;
    JobStatus status;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
}
