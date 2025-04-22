package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "integration_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileEntity extends TimeAwareEntity {

    private Integer totalCount;

    private Integer publishedSuccessCount = 0;
    private Integer publishedFailCount = 0;

    private Integer consumedSuccessCount = 0;
    private Integer consumedFailCount = 0;

    private Integer publisherThroughput; // - total records / time (at completion - success or failure) - calculate on api call
    private Integer consumerThroughput; // - total records / time (at completion - success or failure) - calculate on api call

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;
}