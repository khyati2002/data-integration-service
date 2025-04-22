package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;

@Getter
@Setter
public class FileEntityRequestDto implements Serializable {
    String id;
    JsonNode extendedAttributes;
    Integer totalCount;
}
