package com.salescode.dim.event;

import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

public class EventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaProducer<String, EventListenerDTO> producer;
    private final String topicName;
    private final MailboxExecutor executor;

    public EventPublisher(Properties kafkaProps, String topicName, MailboxExecutor flinkExecutor) {
        this.producer = new KafkaProducer<>(kafkaProps);
        this.topicName = topicName;
        this.executor = flinkExecutor;
    }

    public void publishEventAsync(String requestId, String modelClass, String lob,
                                  Set<Change<Serializable>> changes, ActionType operation, String id) {

        EventListenerDTO dto = new EventListenerDTO(requestId, modelClass, lob, changes, operation, id);
        ProducerRecord<String, EventListenerDTO> record = new ProducerRecord<>(topicName, requestId, dto);

        executor.execute(() -> {
            try {
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        LOG.error("Failed to publish message to Kafka", exception);
                    } else {
                        LOG.info("Published to Kafka topic: {} at offset {}", metadata.topic(), metadata.offset());
                    }
                });
            } catch (Exception e) {
                LOG.error("Error while sending Kafka message", e);
            }
        }, "Kafka async publish");
    }

    public void close() {
        producer.close();
    }
}
