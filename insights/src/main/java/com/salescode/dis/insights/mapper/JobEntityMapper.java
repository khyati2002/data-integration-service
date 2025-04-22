package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobEntityMapper {
    JobEntity toEntity(JobEntityResponseDto jobEntityResponseDto);

    JobEntityResponseDto toDto(JobEntity jobEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobEntity partialUpdate(JobEntityResponseDto jobEntityResponseDto, @MappingTarget JobEntity jobEntity);

    default JobEntity toEntity(JobEntityResponseDto jobEntityResponseDto, String lob, String master){
        JobEntity entity = toEntity(jobEntityResponseDto);
        entity.setLob(lob);
        entity.setMaster(master);
        return entity;
    }
}