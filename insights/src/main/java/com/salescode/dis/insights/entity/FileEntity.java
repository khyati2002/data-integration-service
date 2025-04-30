package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "integration_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileEntity extends TimeAwareEntity {
    @Builder.Default
    private Integer totalCount = 0;

    @Builder.Default
    private Integer publishedSuccessCount = 0;

    @Builder.Default
    private Integer publishedFailCount = 0;

    @Builder.Default
    private Integer consumedSuccessCount = 0;

    @Builder.Default
    private Integer consumedFailCount = 0;

    @Builder.Default
    private Integer serverFailCount = 0;

    @Builder.Default
    private Integer logicalFailCount = 0;

    private Long publisherThroughput; // - total records / time (at completion - success or failure) - calculate on api call
    private Long consumerThroughput; // - total records / time (at completion - success or failure) - calculate on api call

    @Enumerated(EnumType.STRING)
    private FileStatus publishedStatus;

    @Enumerated(EnumType.STRING)
    private FileStatus consumedStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if(this.publishedStatus == FileStatus.COMPLETED){
            setEndTime(Instant.now());
            setPublisherThroughput((this.publishedSuccessCount + this.getPublishedFailCount()) / (this.getEndTime().getEpochSecond() - this.getStartTime().getEpochSecond()));
        }
        if(this.consumedStatus == FileStatus.COMPLETED){
            setEndTime(Instant.now());
            setConsumerThroughput((this.consumedSuccessCount + this.getConsumedFailCount()) / (this.getEndTime().getEpochSecond() - this.getStartTime().getEpochSecond()));
        }
    }
}