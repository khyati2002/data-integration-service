package com.salescode.dis.insights.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StageDefinition {
    private String name;
    private String description;
    private String actionToBeTaken;
    private String howToCheckIssue;
} 