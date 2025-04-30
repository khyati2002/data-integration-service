package com.salescode.dis.insights.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class FileProgressRequest {
    @Min(0) private Integer consumerSuccessCount;
    @Min(0) private Integer consumerFailCount;
    @Min(0) private Integer publishedSuccessCount;
    @Min(0) private Integer publishedFailCount;
    @Min(0) private Integer serverFailCount;
    @Min(0) private Integer logicalFailCount;
}