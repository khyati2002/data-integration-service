package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.FileStatus;

import java.io.Serializable;
import java.time.Instant;

public class FileEntityRequestDto implements Serializable {
    String id;
    String lob;
    JsonNode extendedAttributes;
    Integer totalCount;
}
