package com.salescode.dis.insights.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRequest {
    @NotBlank
    private String fileId;
    private String source;
    private Integer totalCount;
    private Integer publishedSuccess;
    private Integer publishedFailure;
}