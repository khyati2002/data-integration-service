package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.FileStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FileResponse {
    private String fileId;
    private String source;
    private Integer totalCount;
    private Integer publishedSuccessCount;
    private Integer consumedSuccessCount;
    private Integer publishedFailCount;
    private Integer consumedFailCount;
    private FileStatus status;
    private Long jobId;
}