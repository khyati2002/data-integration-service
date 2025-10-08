package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
public class MasterCard {

    private String masterName;
    private ModeOfIntegration mode;
    private Long pendingJobCount;
    private Long completedJobCount;
    private Long failedJobCount;
    private Long saveCount;
    private ProgressStage startStage;
    private Long startStageCount;


}