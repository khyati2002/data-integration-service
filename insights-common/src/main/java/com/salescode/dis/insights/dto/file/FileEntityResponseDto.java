package com.salescode.dis.insights.dto.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.dto.file.stage.FileStageMetricsDto;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileEntityResponseDto implements Serializable {
    String id;
    String fileId;
    String master;
    ProgressStatus status;
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