package com.salescode.dis.insights.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "file_stage_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "stage_name"}))
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

    private Long minProcessingTimeMs;

    private Long maxProcessingTimeMs;

    @Column(nullable = false, name = "stage_name")
    private String stageName;
} 