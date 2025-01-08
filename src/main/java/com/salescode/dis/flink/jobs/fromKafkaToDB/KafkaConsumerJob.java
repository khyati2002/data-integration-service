package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dis.flink.aggregator.ListAggregator;
import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sinks.JOOQSink;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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

    @Value("${app.kafka.batch.timeout.ms:1000}")
    private long batchTimeoutMs;

    @Value("${app.kafka.batch.size:3}")
    private long batchSize;

    public void executeJob() throws Exception {
        int parallel = 8;

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallel);

        final KafkaSource<ObjectNode> kafkaSource = kafkaSourceBuilder.build();
        OutputTag<String> deadLetterTag = new OutputTag<String>(deadLetterTopicName) {};

        SingleOutputStreamOperator<List<CommonDataModel>> sourceStream =
                env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Integration Kafka Source")
                        .setParallelism(parallel)
                        .windowAll(TumblingProcessingTimeWindows.of(Time.milliseconds(batchTimeoutMs)))
                        .aggregate(new ListAggregator<ObjectNode>())
                        .map(s->{
                            log.info("Batch size processing {}", s.size());
                            return s;
                        })
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