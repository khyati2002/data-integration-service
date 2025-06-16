package com.salescode.dis.insights.exception.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.salescode.dis.insights.entity.mapped.CommonEntity.YYYY_MM_DD_HH_MM_SS;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Value
public class ApiError {

    String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYY_MM_DD_HH_MM_SS, timezone = "UTC")
    Instant timestamp = Instant.now();
}
