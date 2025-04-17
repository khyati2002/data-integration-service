package com.salescode.dis.insights.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StatusUpdateRequest {
    @NotNull private String status;
}