package com.salescode.dis.insights.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FileProgressRequest {
    private ConsumerMetrics consumer;
    private PublisherMetrics publisher;

    @Data
    public static class ConsumerMetrics {
        @Min(0)
        private Integer successCount = 0;

        @Min(0)
        private Integer serverFailCount = 0;

        @Min(0)
        private Integer logicalFailCount = 0;

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