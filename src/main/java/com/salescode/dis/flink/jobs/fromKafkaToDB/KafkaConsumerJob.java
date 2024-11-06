package com.salescode.dis.flink.jobs.fromKafkaToDB;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sinks.JOOQSink;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import com.salescode.dis.jooq.generated.tables.pojos.CkOrder;

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
        
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        final KafkaSource<ObjectNode> kafkaSource = kafkaSourceBuilder.build();
        OutputTag<String> deadLetterTag = new OutputTag<String>(deadLetterTopicName) {};

        // Define Input Stream
        //TODO: SingleOutputStreamOperator<CkOrder> should be SingleOutputStreamOperator<POJOBase>
        SingleOutputStreamOperator<CkOrder> sourceStream =
            env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Integration Kafka Source")
                .map((MapFunction<ObjectNode, Tuple2<String, ObjectNode>>) value -> {
                    String entityName = value.at("/transformerInfo/0/entityName").asText();
                    return new Tuple2<>(entityName, value);
                }, TypeInformation.of(new TypeHint<Tuple2<String, ObjectNode>>(){}))
                .keyBy(tuple -> tuple.f0)
                .process(new MessageProcessFunction(deadLetterTag));
            

        System.out.println(url+","+user+","+password);
        System.out.println("--------------------");
            // //Main output to JDBC
        sourceStream.sinkTo(new JOOQSink(url, user, password));

        //Side output to DLQ
        sourceStream.getSideOutput(deadLetterTag).sinkTo(kafkaSinkBuilder.build(deadLetterTopicName));
        
        // Execute Flink environment
        env.execute("Integration Consumer Job");
    }
}
