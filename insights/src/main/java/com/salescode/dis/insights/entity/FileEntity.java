package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "integration_file")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileEntity {

    @Id
    private String fileId;

    private String source;
    private Integer totalCount;
    private Integer publishedSuccessCount = 0;
    private Integer publishedFailCount    = 0;
    private Integer consumerSuccessCount  = 0;
    private Integer consumerFailCount     = 0;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;
}