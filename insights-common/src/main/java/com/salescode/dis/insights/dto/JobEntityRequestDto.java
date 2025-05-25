package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.io.Serializable;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobEntityRequestDto implements Serializable {
    JsonNode extendedAttributes;
    String publisherJobUri;
    String consumerJobUri;
}
