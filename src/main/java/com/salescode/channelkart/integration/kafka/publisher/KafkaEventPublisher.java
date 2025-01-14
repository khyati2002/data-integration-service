package com.salescode.channelkart.integration.kafka.publisher;

import com.salescode.channelkart.dto.StreamingEventData;
import com.salescode.channelkart.models.CommonDataModel;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KafkaEventPublisher implements Publisher<StreamingEventData<List<CommonDataModel>>> {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    Producer<String, StreamingEventData<List<CommonDataModel>>> producer = ProducerCreator.createProducerEvent();

    @Override
    public void publish(String topic, StreamingEventData<List<CommonDataModel>> sd) {
        final ProducerRecord<String, StreamingEventData<List<CommonDataModel>>> record = new ProducerRecord(topic,
                sd.getRequestId(),
                sd);
        try {
            producer.send(record);
        } catch (Exception e) {
            log.error("stacktrace", e);
        }

    }

    @Override
    public void publish(String topic, StreamingEventData<List<CommonDataModel>> sd,
                        int partitionCount) {
        //default implementation
    }
}
