package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.function.DoubleConsumer;

@Entity
@Table(name = "integration_file",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fileId", "master"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileEntity extends TimeAwareEntity {

    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false, updatable = false)
    private String master;

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

    @Column(precision = 10, scale = 2)
    private Double publisherThroughput; // - total records / time (at completion - success or failure) - calculate on api call

    @Column(precision = 10, scale = 2)
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
        if(this.fileId == null){
            this.fileId = this.getId();
        }
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        long elapsedSeconds = Math.max(Duration.between(getStartTime(), getLastModifiedTime()).getSeconds(), 1);
        updateThroughputIfFinal(publishedStatus, publishedSuccessCount + publishedFailCount, elapsedSeconds, this::setPublisherThroughput);
        updateThroughputIfFinal(consumedStatus, consumedSuccessCount + consumedFailCount, elapsedSeconds, this::setConsumerThroughput);
        if(this.publishedStatus == FileStatus.COMPLETED && this.consumedStatus == FileStatus.COMPLETED){
            setEndTime(Instant.now());
        }
    }

    private void updateThroughputIfFinal(FileStatus status, int totalCount, long elapsedSeconds, DoubleConsumer setter) {
        if (status == FileStatus.COMPLETED || status == FileStatus.FAILED) {
            double throughput = (double) totalCount / elapsedSeconds;
            setter.accept(throughput);
        }
    }
}