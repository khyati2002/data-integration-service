// CommonEntity.java
package com.salescode.dis.insights.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dis.insights.config.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Access(AccessType.FIELD)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@MappedSuperclass
public abstract class CommonEntity {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "creation_time", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYY_MM_DD_HH_MM_SS, timezone = "UTC")
    @Builder.Default
    private Instant creationTime = Instant.now();

    @Column(name = "last_modified_time", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYY_MM_DD_HH_MM_SS, timezone = "UTC")
    @Builder.Default
    private Instant lastModifiedTime = Instant.now();

    @Column(name = "lob", length = 50, nullable = false)
    private String lob;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "extended_attributes", columnDefinition = "json")
    private JsonNode extendedAttributes;

    @PrePersist
    protected void onCreate() {
        if (creationTime == null) {
            creationTime = Instant.now();
        }
        lastModifiedTime = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedTime = Instant.now();
    }
}