package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.JobEntityRequestDto;
import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobEntityMapper {

    JobEntity toEntity(JobEntityRequestDto jobEntityRequestDto);

    JobEntityResponseDto toDto(JobEntity jobEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobEntity partialUpdate(JobEntityResponseDto jobEntityResponseDto, @MappingTarget JobEntity jobEntity);

    default JobEntity toEntity(JobEntityRequestDto jobEntityRequestDto, String lob, String master) {
        JobEntity entity = toEntity(jobEntityRequestDto);
        entity.setLob(lob);
        entity.setMaster(master);
        return entity;
    }
}