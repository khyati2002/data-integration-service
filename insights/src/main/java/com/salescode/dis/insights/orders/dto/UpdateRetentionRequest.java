package com.salescode.dis.insights.orders.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
public class UpdateRetentionRequest {

    // Getters and Setters
    @NotNull(message = "Retention hours is required")
    @Min(value = 1, message = "Retention hours must be at least 1")
    private Integer retentionHours;

    @NotBlank(message = "Updated by is required")
    private String updatedBy;


}
