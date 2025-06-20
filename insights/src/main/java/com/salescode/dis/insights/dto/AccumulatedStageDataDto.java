package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccumulatedStageDataDto {
    private ProgressStage stageType;
    private ModeOfIntegration modeOfIntegration;
    private Long totalSuccessCount;
    private Long totalFailureCount;
}