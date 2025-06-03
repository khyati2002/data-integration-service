package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.FileStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
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
    private Long totalCount = 0L;

    @Builder.Default
    private Long publishedSuccessCount = 0L;

    @Builder.Default
    private Long publishedFailCount = 0L;

    @Builder.Default
    private Long consumedSuccessCount = 0L;

    @Builder.Default
    private Long consumedFailCount = 0L;

    @Builder.Default
    private Long serverFailCount = 0L;

    @Builder.Default
    private Long logicalFailCount = 0L;

    @Builder.Default
    private Long retryCount = 0L;

    @Column(precision = 10, scale = 2)
    private BigDecimal publisherThroughput; // - total records / time (at completion - success or failure) - calculate on api call

    @Column(precision = 10, scale = 2)
    private BigDecimal consumerThroughput; // - total records / time (at completion - success or failure) - calculate on api call

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

    private Long minProcessingTimeMs;

    private Long maxProcessingTimeMs;

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
        if((this.publishedStatus == FileStatus.COMPLETED_SUCCESSFULLY || this.publishedStatus == FileStatus.COMPLETED_WITH_FAILURES) && (this.consumedStatus == FileStatus.COMPLETED_SUCCESSFULLY || this.consumedStatus == FileStatus.COMPLETED_WITH_FAILURES)){
            setEndTime(Instant.now());
        }
    }

    private void updateThroughputIfFinal(FileStatus status, Long totalCount, Long elapsedSeconds, Consumer<BigDecimal> setter) {
        if (status == FileStatus.COMPLETED_SUCCESSFULLY || status == FileStatus.COMPLETED_WITH_FAILURES || status == FileStatus.FAILED) {
            BigDecimal throughput = BigDecimal.valueOf(totalCount).divide(BigDecimal.valueOf(elapsedSeconds), new MathContext(2));
            setter.accept(throughput);
        }
    }
}