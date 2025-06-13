package com.salescode.dis.insights.entity;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stage_metadata", uniqueConstraints = @UniqueConstraint(columnNames = {"mode", "stage_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeOfIntegration mode;

    @Column(nullable = false, name = "stage_name")
    private String stageName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String actionToTake;
} 