package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.SSE.DataChangeListener;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@EntityListeners({DataChangeListener.class})
@Table(name = "file_stage_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "stage_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileStageMetrics extends TimeAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(nullable = false, updatable = false)
    private String master;

    @Builder.Default
    private Long successCount = 0L;

    @Builder.Default
    private Long serverFailureCount = 0L;

    @Builder.Default
    private Long logicalFailureCount = 0L;

    @Column(precision = 10, scale = 2)
    private BigDecimal throughput;

    private Integer minProcessingTimeMs;

    private Integer maxProcessingTimeMs;

    @Enumerated(EnumType.STRING)
    private ProgressStage stageType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProgressStatus progressStatus = ProgressStatus.PENDING;

    @Column(nullable = false, updatable = false, name = "mode")
    @Enumerated(EnumType.STRING)
    private ModeOfIntegration modeOfIntegration;

    public Long getTotal() {
        return successCount + logicalFailureCount + serverFailureCount;
    }

    public ProgressStatus getCurrentStatus() {
        if(getTotal() <= 0){
            throw new IllegalArgumentException("total cannot be less than or equal to zero");
        }

        if (Objects.equals(this.getSuccessCount(), getTotal())) {
            return ProgressStatus.COMPLETED_SUCCESSFULLY;
        } else if (Objects.equals(this.getTotal(), getTotal())) {
            return ProgressStatus.COMPLETED_UNSUCCESSFULLY;
        } else {
            return ProgressStatus.FAILED;
        }
    }

}