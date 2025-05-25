package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileProgressRequest {
    private ConsumerMetrics consumer;
    private PublisherMetrics publisher;

    private Long processingTimeMs;

    @JsonIgnore
    private transient Long minProcessingTimeMs;

    @JsonIgnore
    private transient Long maxProcessingTimeMs;

    public static FileProgressRequest createNewInstance() {
        FileProgressRequest newProgress = new FileProgressRequest();
        newProgress.setConsumer(new FileProgressRequest.ConsumerMetrics());
        newProgress.setPublisher(new FileProgressRequest.PublisherMetrics());
        return newProgress;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConsumerMetrics {
        @Min(0)
        private Long successCount = 0L;

        @Min(0)
        private Long serverFailCount = 0L;

        @Min(0)
        private Long logicalFailCount = 0L;

        @Min(0)
        private Long retryCount = 0L;

        public Long getTotalFailCount() {
            return (serverFailCount != null ? serverFailCount : 0) + (logicalFailCount != null ? logicalFailCount : 0);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PublisherMetrics {
        @Min(0)
        private Long successCount = 0L;

        @Min(0)
        private Long failCount = 0L;
    }
}