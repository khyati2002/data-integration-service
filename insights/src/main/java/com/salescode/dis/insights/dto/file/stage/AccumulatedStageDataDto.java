package com.salescode.dis.insights.dto.file.stage;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccumulatedStageDataDto {
    private ProgressStage stageType;
    private ModeOfIntegration modeOfIntegration;
    private Long totalSuccessCount;
    private Long totalFailureCount;
}