package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.JobRequest;
import com.salescode.dis.insights.dto.JobResponse;
import com.salescode.dis.insights.entity.JobEntity; // Assuming you have this entity
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring") // Generate a Spring Bean
public interface JobMapper {

    // Optional: If you need a static instance sometimes
    // JobMapper INSTANCE = Mappers.getMapper(JobMapper.class);

    // Map JobEntity -> JobResponse
    // Assumes JobEntity has fields: id, lob, master, status, totalFileCount, completedFiles, failedFiles
    // If field names differ, use @Mapping(source="entityField", target="dtoField")
    JobResponse jobEntityToJobResponse(JobEntity jobEntity);

    // Map JobRequest -> JobEntity (useful for the service layer)
    // Assumes JobEntity has fields: lob, master
    JobEntity jobRequestToJobEntity(JobRequest jobRequest);

    // If you need to update an existing entity from a request (example)
    // @Mapping(target = "id", ignore = true) // Don't map ID during update
    // void updateJobEntityFromRequest(JobRequest jobRequest, @MappingTarget JobEntity jobEntity);
}