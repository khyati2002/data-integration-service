package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "integration_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JobEntity extends TimeAwareEntity {

    private String master;

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
}