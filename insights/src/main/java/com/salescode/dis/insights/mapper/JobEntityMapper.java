package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.job.JobEntityRequestDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobEntityMapper {

    JobEntity toEntity(JobEntityRequestDto jobEntityRequestDto);

    JobEntityResponseDto toDto(JobEntity jobEntity);

    default JobEntity toEntity(JobEntityRequestDto jobEntityRequestDto, String lob) {
        JobEntity entity = toEntity(jobEntityRequestDto);
        entity.setLob(lob);
        return entity;
    }
}