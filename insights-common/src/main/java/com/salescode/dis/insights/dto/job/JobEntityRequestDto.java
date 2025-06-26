package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
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
    JsonNode extendedAttributes;
    String publisherJobUri;
    String consumerJobUri;
}
