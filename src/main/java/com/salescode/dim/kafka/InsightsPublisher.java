package com.salescode.dim.kafka;

import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.StreamingRawData;
import com.salescode.dim.event.EventPublisher;
import com.salescode.dim.utils.EventListenerDTO;
import com.salescode.dim.utils.InsightsUtils;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.enums.ProgressStage;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;



public class InsightsPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaProducer<String, FileProgressEvent> producer;
    private final String topicName;
    private MailboxExecutor executor;

    public InsightsPublisher(Properties kafkaProps, String topicName, MailboxExecutor flinkExecutor) {
        this.producer = new KafkaProducer<>(kafkaProps);
        this.topicName = topicName;
        this.executor = flinkExecutor;
    }


    public void publishEventAsync(StreamingRawData streamingRawData,long successCount, long logicalFailureCount, long serverFailureCount){

        FileProgressEvent event = InsightsUtils.createRequest(streamingRawData,successCount,logicalFailureCount,serverFailureCount, ProgressStage.SAVE);
        ProducerRecord<String, FileProgressEvent> record = new ProducerRecord<>(topicName, event.getFileId(), event);

        executor.execute(() -> {
            try {
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        LOG.error("Failed to publish message to Kafka topic insights", exception);
                    } else {
                        LOG.info("Published to Kafka topic insights : {} at offset {}", metadata.topic(), metadata.offset());
                    }
                });
            } catch (Exception e) {
                LOG.error("Error while sending Kafka message to insights", e);
            }
        }, "Kafka async publish");
    }


    public void close() {
        producer.close();
    }

}
