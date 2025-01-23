package com.applicate.services.channelkart.integration.kafka.publisher;

import com.applicate.services.channelkart.dto.StreamingRawData;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
@DependsOn("iKafkaConstants")
public class KafkaIntegrationPublisher implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(KafkaIntegrationPublisher.class);
    private static final Map<String, Boolean> TOPIC_MAP = new ConcurrentHashMap<>();
    private static KafkaIntegrationPublisher publisher;
    Producer<String, StreamingRawData> producer = null;

    private KafkaIntegrationPublisher() {
        producer = ProducerCreator.createProducerIntegrations();
    }

    private KafkaIntegrationPublisher(String brokers) {
        producer = ProducerCreator.createProducerIntegrations(brokers);
    }

    public static synchronized KafkaIntegrationPublisher getInstance() {
        if (publisher == null) {
            publisher = new KafkaIntegrationPublisher();
        }
        return publisher;
    }

    public static synchronized void setInstance(KafkaIntegrationPublisher publisherBean) {
        publisher = publisherBean;
    }

    public static synchronized KafkaIntegrationPublisher getInstance(String brokers) {
        if (publisher == null) {
            publisher = new KafkaIntegrationPublisher(brokers);
        }
        return publisher;
    }

    public static synchronized KafkaIntegrationPublisher getNewInstance(String kafkaUrl) {
        return new KafkaIntegrationPublisher(kafkaUrl);
    }

    private void ensureTopic(String topic) {
        if (!TOPIC_MAP.containsKey(topic)) {
            synchronized (TOPIC_MAP) {
                if (!TOPIC_MAP.containsKey(topic)) {
                    IKafkaConstants.createTopic(topic);
                    TOPIC_MAP.put(topic, true);
                }
            }
        }
    }

    public void publish(String topic, StreamingRawData sd) {
        ensureTopic(topic);
        final ProducerRecord<String, StreamingRawData> srRecord = new ProducerRecord<>(topic, 0,
                sd.getRequestId(),
                sd);
        try {
            producer.send(srRecord);
        } catch (Exception e) {
            log.error("", e);
        }

    }

    public Future<RecordMetadata> waitTillPublish(String topic, StreamingRawData sd) {
        ensureTopic(topic);
        final ProducerRecord<String, StreamingRawData> srRecord = new ProducerRecord<>(topic, 0,
                sd.getRequestId(),
                sd);
        try {
            return producer.send(srRecord);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public boolean publishRec(String topic, StreamingRawData sd) {
        ensureTopic(topic);
        final ProducerRecord<String, StreamingRawData> srRecord = new ProducerRecord<>(topic, 0,
                sd.getRequestId(),
                sd);
        try {
            producer.send(srRecord);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish record {}", sd.getRequestId(), e);
        }
        return false;
    }

    public void close() {
        producer.flush();
        producer.close(Duration.ofMinutes(5));
    }

    public void publish(String topic, StreamingRawData sd, int partitionCount) {
        ensureTopic(topic);
        final ProducerRecord<String, StreamingRawData> srRecord = new ProducerRecord<>(topic, Math.abs(sd.getRequestId().hashCode() % partitionCount),
                sd.getRequestId(),
                sd);
        try {
            producer.send(srRecord);
        } catch (Exception e) {
            log.error("stacktrace", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setInstance(this);
    }
}
