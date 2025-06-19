package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AggregationWrapper {

    @Getter
    private final FileProgressEvent aggregatedEvent;
    private final List<FileProgressEvent> originalEventsInBatch = new ArrayList<>();

    public AggregationWrapper(FileProgressEvent seedEvent) {
        this.aggregatedEvent = createEmptyAggregatedEvent(seedEvent);
    }

    public void addEvent(FileProgressEvent event) {
        originalEventsInBatch.add(event);
        merge(event);
    }

    public List<FileProgressEvent> getOriginalEvents() {
        return originalEventsInBatch;
    }

    private void merge(FileProgressEvent source) {
        FileProgressRequest existing = aggregatedEvent.getProgress();
        FileProgressRequest incoming = source.getProgress();

        existing.setSuccessCount(safeSum(existing.getSuccessCount(), incoming.getSuccessCount()));
        existing.setServerFailureCount(safeSum(existing.getServerFailureCount(), incoming.getServerFailureCount()));
        existing.setLogicalFailureCount(safeSum(existing.getLogicalFailureCount(), incoming.getLogicalFailureCount()));
        existing.setMinProcessingTimeMs(min(existing.getMinProcessingTimeMs(), incoming.getMinProcessingTimeMs()));
        existing.setMaxProcessingTimeMs(max(existing.getMaxProcessingTimeMs(), incoming.getMaxProcessingTimeMs()));
        if (existing.getStageType() == null) existing.setStageType(incoming.getStageType());

        if (aggregatedEvent.getLob() == null) aggregatedEvent.setLob(source.getLob());
        if (aggregatedEvent.getJobId() == null) aggregatedEvent.setJobId(source.getJobId());
        if (aggregatedEvent.getMasterName() == null) aggregatedEvent.setMasterName(source.getMasterName());
    }

    private FileProgressEvent createEmptyAggregatedEvent(FileProgressEvent source) {
        var newEvent = new FileProgressEvent();
        newEvent.setFileId(source.getFileId());
        newEvent.setMasterName(source.getMasterName());
        newEvent.setJobId(source.getJobId());
        newEvent.setLob(source.getLob());
        newEvent.setProgress(new FileProgressRequest());
        return newEvent;
    }

    private Long safeSum(Long a, Long b) {
        return Optional.ofNullable(a).orElse(0L) + Optional.ofNullable(b).orElse(0L);
    }

    private Long min(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private Integer min(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private Long max(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private Integer max(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }
}
