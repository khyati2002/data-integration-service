package com.salescode.dis.insights.dto.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.dto.file.stage.FileStageMetricsDto;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link com.salescode.dis.insights.entity.FileEntity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityResponseDto implements Serializable {
    String id;
    String fileId;
    String master;
    Instant creationTime;
    Instant lastModifiedTime;
    String lob;
    JsonNode extendedAttributes;
    Instant startTime;
    Instant endTime;
    Long totalCount;
    ModeOfIntegration modeOfIntegration;
    List<FileStageMetricsDto> stageMetrics;
}