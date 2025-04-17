package com.salescode.dis.insights.mapper;

import com.salescode.dis.insights.dto.FileRequest;
import com.salescode.dis.insights.dto.FileResponse;
import com.salescode.dis.insights.entity.FileEntity; // Assuming you have this entity
import com.salescode.dis.insights.entity.JobEntity; // Assuming FileEntity has a JobEntity field
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring") // Generate a Spring Bean
public interface FileMapper {

    // Optional: If you need a static instance sometimes
    // FileMapper INSTANCE = Mappers.getMapper(FileMapper.class);

    // Map FileEntity -> FileResponse
    // Handle potential differences and nested objects
    @Mapping(source = "job.id", target = "jobId") // Map nested JobEntity's ID to jobId
    @Mapping(source = "consumerSuccessCount", target = "consumedSuccessCount") // Handle naming difference
    @Mapping(source = "consumerFailCount", target = "consumedFailCount") // Handle naming difference
    FileResponse fileEntityToFileResponse(FileEntity fileEntity);

    // Map FileRequest -> FileEntity (useful for the service layer)
    @Mapping(source = "publishedSuccess", target = "publishedSuccessCount") // Handle naming difference
    @Mapping(source = "publishedFailure", target = "publishedFailCount") // Handle naming difference
    @Mapping(target = "fileId", ignore = true) // Usually generated
    @Mapping(target = "job", ignore = true) // Should be set separately in service
    @Mapping(target = "status", ignore = true) // Usually set separately
    @Mapping(target = "consumerSuccessCount", ignore = true) // Not in request
    @Mapping(target = "consumerFailCount", ignore = true) // Not in request
    FileEntity fileRequestToFileEntity(FileRequest fileRequest);

    // Helper method if needed, though MapStruct handles job.id directly
    // @Named("jobEntityToJobId")
    // default Long jobEntityToJobId(JobEntity job) {
    //     return job == null ? null : job.getId();
    // }
}