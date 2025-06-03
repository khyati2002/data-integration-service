package com.salescode.dim.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FileProgressEvent implements Serializable {
    private final long timestamp = System.currentTimeMillis();
    private String eventId;
    private String fileId;
    private String jobId;
    private String lob;
    private String masterName;
    private String errorMessage;
    private FileProgressRequest progress;

    public static FileProgressEvent createInsightsConsumerDto(String eventId, String fileId, String jobId, String lob, String master, String errorMessage, Integer consumedSuccess, Integer consumedFailure) {
        FileProgressEvent event = new FileProgressEvent();
        event.setEventId(eventId);
        event.setFileId(fileId);
        event.setJobId(jobId);
        event.setLob(lob);
        event.setMasterName(master);
        event.setErrorMessage(errorMessage);
        FileProgressRequest progress = FileProgressRequest.createNewInstance();
        progress.getConsumer().setSuccessCount(consumedSuccess);
        progress.getConsumer().setLogicalFailCount(consumedFailure);
        event.setProgress(progress);
        return event;
    }

    public static FileProgressEvent createInsightsPublisherDto(String eventId, String fileId, String jobId, String lob, String master, String errorMessage, Integer publishedSuccess, Integer publishedFailure) {
        FileProgressEvent event = new FileProgressEvent();
        event.setEventId(eventId);
        event.setFileId(fileId);
        event.setJobId(jobId);
        event.setLob(lob);
        event.setMasterName(master);
        event.setErrorMessage(errorMessage);
        FileProgressRequest progress = FileProgressRequest.createNewInstance();
        progress.getPublisher().setSuccessCount(publishedSuccess);
        progress.getPublisher().setFailCount(publishedFailure);
        event.setProgress(progress);
        return event;
    }


    @Getter
    @Setter
    public static class FileProgressRequest {
        private ConsumerMetrics consumer;
        private PublisherMetrics publisher;

        public static FileProgressRequest createNewInstance() {
            FileProgressRequest newProgress = new FileProgressRequest();
            newProgress.setConsumer(new FileProgressRequest.ConsumerMetrics());
            newProgress.setPublisher(new FileProgressRequest.PublisherMetrics());
            return newProgress;
        }

        @Getter
        @Setter
        public static class ConsumerMetrics {
            private Integer successCount = 0;

            private Integer serverFailCount = 0;

            private Integer logicalFailCount = 0;

        }

        @Getter
        @Setter
        public static class PublisherMetrics {
            private Integer successCount = 0;

            private Integer failCount = 0;
        }
    }
}