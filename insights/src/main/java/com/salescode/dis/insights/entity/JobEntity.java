package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.SSE.DataChangeListener;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners({DataChangeListener.class})
@Table(name = "integration_job", indexes = {
    @Index(name = "idx_job_lob_master", columnList = "lob, start_time desc")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JobEntity extends TimeAwareEntity {

    private String publisherJobUri;

    private String consumerJobUri;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.PENDING;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if(this.status == ProgressStatus.COMPLETED_SUCCESSFULLY || status == ProgressStatus.COMPLETED_UNSUCCESSFULLY || this.status == ProgressStatus.FAILED){
            setEndTime(Instant.now());
        }
    }
}