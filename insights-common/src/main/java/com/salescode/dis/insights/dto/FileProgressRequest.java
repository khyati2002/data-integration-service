package com.salescode.dis.insights.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProgressRequest {
    private ConsumerMetrics consumer;
    private PublisherMetrics publisher;

    private Long processingTimeMs;

    @JsonIgnore
    private transient Long minProcessingTimeMs;

    @JsonIgnore
    private transient Long maxProcessingTimeMs;

    @Data
    public static class ConsumerMetrics {
        @Min(0)
        private Integer successCount = 0;

        @Min(0)
        private Integer serverFailCount = 0;

        @Min(0)
        private Integer logicalFailCount = 0;

        @Min(0)
        private Integer retryCount = 0;

        public Integer getTotalFailCount() {
            return (serverFailCount != null ? serverFailCount : 0) + 
                   (logicalFailCount != null ? logicalFailCount : 0);
        }
    }

    @Data
    public static class PublisherMetrics {
        @Min(0)
        private Integer successCount = 0;

        @Min(0)
        private Integer failCount = 0;
    }

    public static FileProgressRequest createNewInstance(){
        FileProgressRequest newProgress = new FileProgressRequest();
        newProgress.setConsumer(new FileProgressRequest.ConsumerMetrics());
        newProgress.setPublisher(new FileProgressRequest.PublisherMetrics());
        return newProgress;
    }
}