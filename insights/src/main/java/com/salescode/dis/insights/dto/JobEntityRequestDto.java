package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class JobEntityRequestDto implements Serializable {
    String lob;
    JsonNode extendedAttributes;
    String master;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
}
