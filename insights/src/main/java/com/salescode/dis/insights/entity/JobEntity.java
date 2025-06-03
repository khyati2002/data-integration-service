package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "integration_job", indexes = {
    @Index(name = "idx_job_lob_master", columnList = "lob, start_time desc")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JobEntity extends TimeAwareEntity {


    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private String publisherJobUri;

    private String consumerJobUri;

    @Builder.Default
    private Integer totalFileCount = 0;

    @Builder.Default
    private Integer completedFiles = 0;

    @Builder.Default
    private Integer failedFiles = 0;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FileEntity> files = new ArrayList<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = JobStatus.PENDING;
        }
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if(this.status == JobStatus.COMPLETED_SUCCESSFULLY || this.status == JobStatus.COMPLETED_WITH_FAILURES || this.status == JobStatus.FAILED){
            setEndTime(Instant.now());
        }
        this.totalFileCount = files.size();
    }
}