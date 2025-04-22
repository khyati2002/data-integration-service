package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.FileEntityDto;
import com.salescode.dis.insights.entity.FileEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileEntityMapper {
    FileEntity toEntity(FileEntityDto fileEntityDto);

    FileEntityDto toDto(FileEntity fileEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileEntity partialUpdate(FileEntityDto fileEntityDto, @MappingTarget FileEntity fileEntity);
}