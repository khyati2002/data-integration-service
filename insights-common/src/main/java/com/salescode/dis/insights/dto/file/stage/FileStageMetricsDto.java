package com.salescode.dis.insights.dto.file.stage;

import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileStageMetricsDto implements Serializable {
    ProgressStatus progressStatus;
    String id;
    ProgressStage stageType;
    Long successCount;
    Long serverFailureCount;
    Long logicalFailureCount;
    BigDecimal throughput;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;
} 