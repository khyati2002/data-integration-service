package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.ProgressStage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageDto {
    private String mode;
    private ProgressStage stageName;
    private String description;
    private String actionToBeTaken;
}