package com.salescode.dis.insights.dto.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityRequestDto implements Serializable {
    String fileId;
    JsonNode extendedAttributes;
    @Builder.Default
    Long totalCount = 0L;
    ModeOfIntegration modeOfIntegration;
}
