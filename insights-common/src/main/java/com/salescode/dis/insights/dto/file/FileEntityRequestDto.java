package com.salescode.dis.insights.dto.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityRequestDto implements Serializable {
    @NotNull
    String fileId;
    JsonNode extendedAttributes;
    @NotNull @Builder.Default Long totalCount = 0L;
    @NotNull
    ModeOfIntegration modeOfIntegration;
}
