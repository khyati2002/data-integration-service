package com.salescode.dis.insights.dto.file.stage;

import com.salescode.dis.insights.enums.ProgressStage;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileStageMetricsDto implements Serializable {
    String id;
    ProgressStage stageType;
    Long successCount;
    Long serverFailureCount;
    Long logicalFailureCount;
    BigDecimal throughput;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;
} 