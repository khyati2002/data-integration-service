package com.salescode.dis.insights.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "integration_stage_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IntegrationStageProgress extends TimeAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(nullable = false)
    private String stage;

    @Column(nullable = false)
    private Long successCount;

    @Column(nullable = false)
    private Long failCount;

    @Column(nullable = false)
    private Boolean isCompleted;

    @Column
    private String description;

    @Column
    private String actionToBeTaken;

    @Column
    private String howToCheckIssue;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.successCount == null) {
            this.successCount = 0L;
        }
        if (this.failCount == null) {
            this.failCount = 0L;
        }
        if (this.isCompleted == null) {
            this.isCompleted = false;
        }
    }
} 