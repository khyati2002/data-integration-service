package com.salescode.dis.insights.dto.file.progress;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileProgressRequest implements Serializable {

    ProgressStage stageType;
    @Builder.Default Long successCount = 0L;
    @Builder.Default Long serverFailureCount = 0L;
    @Builder.Default Long logicalFailureCount = 0L;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;
    @NotNull ModeOfIntegration modeOfIntegration;
}