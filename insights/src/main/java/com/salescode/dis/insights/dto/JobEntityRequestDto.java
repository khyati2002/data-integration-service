package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;

public class JobEntityRequestDto implements Serializable {
    String lob;
    JsonNode extendedAttributes;
    String master;
    String publisherJobUri;
    String consumerJobUri;
    Integer totalFileCount;
}
