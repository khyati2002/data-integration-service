package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
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

    private Double publisherThroughput; // - total records / time (at completion - success or failure) - calculate on api call
    private Double consumerThroughput; // - total records / time (at completion - success or failure) - calculate on api call

    @Enumerated(EnumType.STRING)
    private FileStatus publishedStatus;

    @Enumerated(EnumType.STRING)
    private FileStatus consumedStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isApiBased = false;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.consumedStatus == null) {
            this.consumedStatus = FileStatus.PENDING;
        }
        if (this.publishedStatus == null) {
            this.publishedStatus = FileStatus.PENDING;
        }
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if (this.publishedStatus == FileStatus.COMPLETED || this.publishedStatus == FileStatus.FAILED) {
                setEndTime(Instant.now());
                double timeTaken = this.getEndTime().getEpochSecond() - this.getStartTime().getEpochSecond();
                double totalPublished = this.publishedSuccessCount + this.getPublishedFailCount();
                if (getStartTime().toEpochMilli() == getEndTime().toEpochMilli()) {
                    setPublisherThroughput(totalPublished);
                }
                setPublisherThroughput(totalPublished / timeTaken);
        }
        if (this.consumedStatus == FileStatus.COMPLETED || this.consumedStatus == FileStatus.FAILED) {
                setEndTime(Instant.now());
                double timeTaken = this.getEndTime().getEpochSecond() - this.getStartTime().getEpochSecond();
                double totalConsumed = this.consumedSuccessCount + this.getConsumedFailCount();
                if (getStartTime().toEpochMilli() == getEndTime().toEpochMilli()) {
                    setConsumerThroughput(totalConsumed);
                }
                setConsumerThroughput(totalConsumed / timeTaken);
        }

    }
}