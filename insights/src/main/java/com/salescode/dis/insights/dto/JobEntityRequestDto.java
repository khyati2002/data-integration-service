package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import com.salescode.dis.insights.enums.JobStatus;
import lombok.Value;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Value
@Getter
@Setter
public class JobEntityRequestDto implements Serializable {
    JsonNode extendedAttributes;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
}
