package com.salescode.dis.insights.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileStageMetricsDto implements Serializable {
    String id;
    String fileId;
    String stageName;
    Long successCount;
    Long failureCount;
    BigDecimal throughput;
    Long minProcessingTimeMs;
    Long maxProcessingTimeMs;
} 