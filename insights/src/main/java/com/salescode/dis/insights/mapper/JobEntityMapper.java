package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.JobEntityDto;
import com.salescode.dis.insights.entity.JobEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobEntityMapper {
    JobEntity toEntity(JobEntityDto jobEntityDto);

    JobEntityDto toDto(JobEntity jobEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobEntity partialUpdate(JobEntityDto jobEntityDto, @MappingTarget JobEntity jobEntity);

    default JobEntity toEntity(JobEntityDto jobEntityDto, String lob, String master){
        JobEntity entity = toEntity(jobEntityDto);
        entity.setLob(lob);
        entity.setMaster(master);
        return entity;
    }
}