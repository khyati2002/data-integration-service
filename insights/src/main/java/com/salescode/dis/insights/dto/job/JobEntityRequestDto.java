package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

/**
 * DTO for {@link com.salescode.dis.insights.entity.JobEntity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class JobEntityRequestDto implements Serializable {
    JsonNode extendedAttributes;
    String publisherJobUri;
    String consumerJobUri;
//    @NotNull ModeOfIntegration modeOfIntegration;
}
