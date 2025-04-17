package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.JobStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class JobResponse {
    private Long id;
    private String lob;
    private String master;
    private JobStatus status;
    private Integer totalFileCount;
    private Integer completedFiles;
    private Integer failedFiles;
}