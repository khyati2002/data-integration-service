package com.salescode.dis.insights.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "insights_metadata")
public class InsightsMetadata {

    @Id
    private String id;

    @Column(name = "key", nullable = false, length = 255)
    private String key;

    @Column(name = "value", length = 3000)
    private String value;
}
