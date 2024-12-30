package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sinks.JOOQSink;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumerJob {

    @Autowired
    private DISKafkaSourceBuilder kafkaSourceBuilder;

    @Autowired
    private DISKafkaSinkBuilder kafkaSinkBuilder;

    @Value("${app.jobs.from-kafka-to-db.dlq-topic-name}")
    private String deadLetterTopicName;

    @Value("${app.jobs.from-kafka-to-db.db.url}")
    private String url;

    @Value("${app.jobs.from-kafka-to-db.db.user}")
    private String user;

    @Value("${app.jobs.from-kafka-to-db.db.password}")
    private String password;


    public void executeJob() throws Exception {
        int parallel = 8;

        // Set the default parallelism to 8 for the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallel);

        final KafkaSource<ObjectNode> kafkaSource = kafkaSourceBuilder.build();
        OutputTag<String> deadLetterTag = new OutputTag<String>(deadLetterTopicName) {};

        // Define Input Stream with parallelism set to 8
        SingleOutputStreamOperator<CommonDataModel> sourceStream =
                env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Integration Kafka Source")
                        .setParallelism(parallel)  // Set parallelism for the source
                        .rebalance()
                        .process(new MessageProcessFunction(deadLetterTag))
                        .setParallelism(parallel); // Set parallelism for the process function

        System.out.println(url + "," + user + "," + password);
        System.out.println("--------------------");

        // Main output to JDBC with sink parallelism set to 8
        sourceStream.sinkTo(new JOOQSink(url, user, password))
                .setParallelism(parallel);  // Set parallelism for the JDBC sink

        // Side output to Kafka dead-letter topic with sink parallelism set to 8
        sourceStream.getSideOutput(deadLetterTag)
                .sinkTo(kafkaSinkBuilder.build(deadLetterTopicName))
                .setParallelism(parallel);  // Set parallelism for the Kafka sink

        // Execute Flink environment
        env.execute("Integration Consumer Job");
    }
}