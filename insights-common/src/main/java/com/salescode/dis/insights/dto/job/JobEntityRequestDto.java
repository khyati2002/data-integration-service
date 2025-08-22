package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class JobEntityRequestDto implements Serializable {
    String id;
    JsonNode extendedAttributes;
    String publisherJobUri;
    String consumerJobUri;
    ProgressStatus status;
}
