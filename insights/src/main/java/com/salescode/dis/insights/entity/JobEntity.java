package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "integration_job")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lob;
    private String master;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Integer totalFileCount = 0;
    private Integer completedFiles = 0;
    private Integer failedFiles = 0;

    @OneToMany(
        mappedBy = "job",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<FileEntity> files = new ArrayList<>();
}