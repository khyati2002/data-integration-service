package com.salescode.dis.insights.dto.file.progress;

import com.salescode.dis.insights.enums.ProgressStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileProgressRequest implements Serializable {

    ProgressStage stageName;
    @Builder.Default Long successCount = 0L;
    @Builder.Default Long serverFailureCount = 0L;
    @Builder.Default Long logicalFailureCount = 0L;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;

}