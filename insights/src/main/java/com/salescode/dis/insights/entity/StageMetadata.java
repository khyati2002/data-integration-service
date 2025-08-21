package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.sse.DataChangeListener;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@EntityListeners({DataChangeListener.class})
@Table(name = "stage_metadata", uniqueConstraints = @UniqueConstraint(columnNames = {"mode", "stage_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProgressStage stageType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ModeOfIntegration mode;

    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String actionToTake;


}