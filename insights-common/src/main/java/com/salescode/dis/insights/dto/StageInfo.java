package com.salescode.dis.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StageInfo {
    private String stageName;
    private String description;
    private String actionToTake;
} 