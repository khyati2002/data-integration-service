package com.salescode.dim.kafka;

import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.StreamingRawData;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.api.common.operators.MailboxExecutor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

public class FailurePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(FailurePublisher.class);
    private final KafkaProducer<String, StreamingRawData> producer;
    private final String topicName;
    private final MailboxExecutor executor;

    public FailurePublisher(Properties kafkaProps, String topicName, MailboxExecutor flinkExecutor) {
        this.producer = new KafkaProducer<>(kafkaProps);
        this.topicName = topicName;
        this.executor = flinkExecutor;
    }

    public void publishEventAsync(StreamingRawData streamingRawData) {

        ProducerRecord<String, StreamingRawData> record = new ProducerRecord<>(topicName, streamingRawData.getRequestId(), streamingRawData);

        executor.execute(() -> {
            try {
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        LOG.error("Failed to publish failed message to Kafka", exception);
                    } else {
                        LOG.info("Published to Kafka failure topic: {} at offset {}", metadata.topic(), metadata.offset());
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
