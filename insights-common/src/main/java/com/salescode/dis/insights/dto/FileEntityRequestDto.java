package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;


import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityRequestDto implements Serializable {
    String fileId;
    JsonNode extendedAttributes;
    Long totalCount;
}
