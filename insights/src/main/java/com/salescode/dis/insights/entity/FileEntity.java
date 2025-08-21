package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.SSE.DataChangeListener;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.enums.ProgressStatus;
import io.reactivex.rxjava3.internal.util.LinkedArrayList;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Entity
@EntityListeners({DataChangeListener.class})
@Table(name = "integration_file", uniqueConstraints = @UniqueConstraint(columnNames = {"fileId", "master"}))
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


    @Column(nullable = false, updatable = false, name = "mode")
    @Enumerated(EnumType.STRING)
    private ModeOfIntegration modeOfIntegration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileStageMetrics> fileStageMetrics = new ArrayList<>();

    /** calculated based on progress of stages*/
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ProgressStatus status = ProgressStatus.PENDING;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.fileId == null) {
            this.fileId = this.getId();
        }
        if(this.modeOfIntegration == null) {
            this.modeOfIntegration = ModeOfIntegration.CK_API_CLIENT;
        }
        this.setStatus(ProgressStatus.PENDING);
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        // The throughput calculation and status updates will now be handled by individual stage metrics
        // and potentially by a higher-level service that aggregates stage data.
    }
}