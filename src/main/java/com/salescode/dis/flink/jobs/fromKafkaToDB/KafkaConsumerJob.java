package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dis.flink.aggregator.ListAggregator;
import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sinks.JOOQSink;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaConsumerJob {

    transient Logger log = LoggerFactory.getLogger(KafkaConsumerJob.class);

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

    @Value("${app.parallelism:1}")
    private int parallelism;

    public void executeJob() throws Exception {
        int parallel = parallelism;

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallel);

        final KafkaSource<ObjectNode> kafkaSource = kafkaSourceBuilder.build();
        OutputTag<String> deadLetterTag = new OutputTag<String>(deadLetterTopicName) {};

        SingleOutputStreamOperator<List<CommonDataModel>> sourceStream =
                env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Integration Kafka Source")
                        .setParallelism(5)
//                        .windowAll(TumblingProcessingTimeWindows.of(Time.milliseconds(batchTimeoutMs)))
                        .countWindowAll(3)
                        .aggregate(new ListAggregator<ObjectNode>())
                        .map(new MapFunction<List<ObjectNode>, List<ObjectNode>>() {
                            @Override
                            public List<ObjectNode> map(List<ObjectNode> s) throws Exception {
                                log.info("Batch size processing {}", s.size());
                                return (List<ObjectNode>) s;
                            }
                        })
                        .setParallelism(parallel)
                        .process(new MessageProcessFunction(deadLetterTag))
                        .setParallelism(parallel);

        System.out.println(url + "," + user + "," + password);
        System.out.println("--------------------");

        sourceStream.sinkTo(new JOOQSink(url, user, password))
                .setParallelism(parallel);

        sourceStream.getSideOutput(deadLetterTag)
                .sinkTo(kafkaSinkBuilder.build(deadLetterTopicName))
                .setParallelism(parallel);

        env.execute("Integration Consumer Job");
    }
}
