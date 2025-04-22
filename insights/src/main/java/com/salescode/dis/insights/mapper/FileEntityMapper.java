package com.salescode.dis.insights.mapper;


import com.salescode.dis.insights.dto.FileEntityRequestDto;
import com.salescode.dis.insights.dto.FileEntityResponseDto;
import com.salescode.dis.insights.entity.FileEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileEntityMapper {
    FileEntity toEntity(FileEntityRequestDto fileEntityDto);

    FileEntityResponseDto toDto(FileEntity fileEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FileEntity partialUpdate(FileEntityRequestDto fileEntityDto, @MappingTarget FileEntity fileEntity);
}