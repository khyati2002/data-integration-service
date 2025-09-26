package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProgressPublisher {
    private final FileService fileService;
    private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
    @Async("kafkaExecutor")
    public void sendKafkaMessage(
            String topic,
            String fileId,
            FileProgressRequest progress,
            String lob,
            String masterName,
            String eventId
    ) {
        try {
            FileEntity file = fileService.getCached(fileId, masterName);
            FileProgressEvent event = new FileProgressEvent();
            event.setEventId(eventId);
            event.setFileId(fileId);
            event.setLob(lob);
            event.setMasterName(masterName);
            event.setProgress(progress);
            event.setJobId(file.getJob().getId());

            kafkaTemplate.send(topic, fileId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send progress event: eventId={} topic={} fileId={} master={}", eventId, topic, fileId, masterName, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error processing file progress for fileId={} masterName={}", fileId, masterName, e);
        }
        CompletableFuture.completedFuture(null);
    }
}

