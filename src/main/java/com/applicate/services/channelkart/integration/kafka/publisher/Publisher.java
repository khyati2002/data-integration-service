package com.applicate.services.channelkart.integration.kafka.publisher;

public interface Publisher<T> {
    void publish(String topic, T sd);

    void publish(String topic, T sd,int partitionCount);

}
