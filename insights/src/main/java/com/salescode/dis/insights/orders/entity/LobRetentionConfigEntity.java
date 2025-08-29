package com.salescode.dis.insights.orders.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "lob_retention_config")
public class LobRetentionConfigEntity {

    // Getters and Setters
    @Id
    @Column(name = "lob")
    private String lob;

    @NotNull
    @Column(name = "retention_hours", nullable = false)
    private Integer retentionHours = 24;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @NotNull
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    // Constructors
    public LobRetentionConfigEntity() {}

    public LobRetentionConfigEntity(String lob, Integer retentionHours, String updatedBy) {
        this.lob = lob;
        this.retentionHours = retentionHours;
        this.updatedBy = updatedBy;
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = OffsetDateTime.now();
    }

}