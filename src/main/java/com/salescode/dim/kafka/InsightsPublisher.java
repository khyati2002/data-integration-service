package com.salescode.dim.kafka;

import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.event.EventPublisher;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

import static com.salescode.dim.kafka.FileProgressEvent.createInsightsConsumerDto;

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



    public void publishEventAsync(String eventId, String fileId, String jobId, String lob, String master, String errorMessage, Integer consumedSuccess, Integer consumedFailure){

        FileProgressEvent event = createInsightsConsumerDto(eventId, fileId, jobId, lob, master, errorMessage, consumedSuccess, consumedFailure);
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
