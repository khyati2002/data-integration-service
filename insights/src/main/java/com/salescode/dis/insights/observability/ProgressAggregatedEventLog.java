package com.salescode.dis.insights.observability;

import ai.salescode.observability.toolkit.models.ObservabilityEvent;
import com.salescode.dis.insights.enums.ProgressStage;
import io.opentelemetry.api.common.Attributes;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProgressAggregatedEventLog implements ObservabilityEvent {

    private String masterName;
    private String jobId;
    private String lob;
    private String traceId;
    private String message;
    private Integer minProcessingTime;
    private Integer maxProcessingTime;
    private String stageType;
    private Long successCount;
    private Long failureCount;

    @Override
    public String getEventType() {
        return "progress-aggregated-event";
    }

    @Override
    public Attributes toAttributes() {
        return Attributes.builder()
                .put("traceId", traceId)
                .put("masterName", masterName)
                .put("jobId", jobId)
                .put("lob", lob)
                .put("message", message)
                .put("minProcessingTime",minProcessingTime!=null?minProcessingTime:5)
                .put("maxProcessingTime",maxProcessingTime!=null?maxProcessingTime:5)
                .put("stageType", stageType)
                .put("event.type", getEventType())
                .put("successCount",successCount)
                .put("failureCount",failureCount)
                .build();
    }
}
