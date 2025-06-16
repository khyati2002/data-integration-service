package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
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
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    private String publisherJobUri;

    private String consumerJobUri;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if(this.status == JobStatus.COMPLETED || this.status == JobStatus.FAILED){
            setEndTime(Instant.now());
        }
    }
}