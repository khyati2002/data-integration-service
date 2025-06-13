package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.FileStageMetricsDto;
import com.salescode.dis.insights.entity.FileStageMetrics;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileStageMetricsMapper {
    FileStageMetricsDto toDto(FileStageMetrics fileStageMetrics);
} 