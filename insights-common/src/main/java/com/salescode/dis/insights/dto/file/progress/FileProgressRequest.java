package com.salescode.dis.insights.dto.file.progress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescode.dis.insights.enums.ModeOfIntegration;

import com.salescode.dis.insights.enums.ProgressStage;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileProgressRequest implements Serializable {

    ProgressStage stageType;
    @Builder.Default Long successCount = 0L;
    @Builder.Default Long serverFailureCount = 0L;
    @Builder.Default Long logicalFailureCount = 0L;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;
    ModeOfIntegration modeOfIntegration;
}