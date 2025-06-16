package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
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

    @Builder.Default
    private Long successCount = 0L;

    @Builder.Default
    private Long failureCount = 0L;

    @Column(precision = 10, scale = 2)
    private BigDecimal throughput;

    private Integer minProcessingTimeMs;

    private Integer maxProcessingTimeMs;

    @Enumerated(EnumType.STRING)
    private ProgressStage stageType;
} 