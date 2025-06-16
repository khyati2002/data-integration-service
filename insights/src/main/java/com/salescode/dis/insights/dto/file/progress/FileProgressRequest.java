package com.salescode.dis.insights.dto.file.progress;

import com.salescode.dis.insights.enums.ProgressStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileProgressRequest implements Serializable {

    ProgressStage stageName;
    @Builder.Default
    Long successCount = 0L;
    @Builder.Default
    Long failureCount = 0L;
    Integer minProcessingTimeMs;
    Integer maxProcessingTimeMs;

}