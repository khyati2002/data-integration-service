package com.salescode.dis.insights.entity.mapped;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@Access(AccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@MappedSuperclass
public abstract class TimeAwareEntity extends CommonEntity {

    public static final String START_TIME = "startTime";

    @Column(name = "start_time", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYY_MM_DD_HH_MM_SS, timezone = "UTC")
    @Builder.Default
    private Instant startTime = Instant.now();

    @Column(name = "end_time", nullable = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYY_MM_DD_HH_MM_SS, timezone = "UTC")
    private Instant endTime;

}
