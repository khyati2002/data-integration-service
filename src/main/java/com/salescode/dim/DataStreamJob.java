/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.utils.EventListenerDTO;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.TopicSelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.triggers.PurgingTrigger;
import org.apache.flink.streaming.runtime.operators.windowing.TimestampedValue;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.salescode.dim.PropertyLoader.mergeProperties;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class DataStreamJob {

    // Define SideOutputTag for failed records
    public static final OutputTag<StreamingRawData> FAILED_TRANSFORMATIONS = new OutputTag<>("failed-transformations") {
    };

    public static final SerializationSchema<StreamingRawData> recordKeySerializationSchema = (StreamingRawData element) -> element.getRequestId()
            .getBytes();


    private static final Logger LOG = LoggerFactory.getLogger(DataStreamJob.class);

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

      //   env.enableCheckpointing(10000);
      //  env.setParallelism(2);

        // Load the application properties
        final Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(env);
        LOG.info("Application properties: {}", applicationProperties);

        Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());

        // Read from kafka and bifurcate topic on basis of entities and sink to respective topics
        Properties inout0Properties = mergeProperties(applicationProperties.get("InOut0"), commonProperties);
        String inputTopic = inout0Properties.getProperty("input.topic");
        String outTopicPrefix = inout0Properties.getProperty("output.topic.prefix");
        String failureTopic = inout0Properties.getProperty("failure.topic");
        String outTopic = inout0Properties.getProperty("output.topic");
        String bootstrapServers = inout0Properties.getProperty("bootstrap.servers");
        String lob = inout0Properties.getProperty("lob");
        String eventTopic = inout0Properties.getProperty("event.topic");

        KafkaSource<StreamingRawData> kafkaSource = FlinkJobSource.createKafkaSource(inout0Properties, new JsonDeserializationSchema<>(StreamingRawData.class), inputTopic + "-" + lob);

        TopicSelector<StreamingRawData> topicSelector = (StreamingRawData record) -> {
            String topicName = Optional.ofNullable(record.getTransformerInfo())
                    .filter(s -> !s.isEmpty())
                    .map(t -> t.get(0).getEntityName())
                    .filter(s -> !s.isEmpty())
                    .map(entityName -> getEntityTopic(outTopicPrefix, lob, entityName))
                    .orElseGet(() -> {
                        record.setResponses(List.of(new StreamingRawData.Response("Failure", "Could not find entity name in transformerInfo")));
                        return failureTopic + "-" + lob;
                    });
//            KafkaTopicCreator.createTopicIfNotExists(topicName, bootstrapServers, 5, (short) 1);
            return topicName;
        };


        KafkaSink<StreamingRawData> kafkaSink = FlinkJobSink.createKafkaSink(inout0Properties, recordKeySerializationSchema, topicSelector);

        env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka source")
                .sinkTo(kafkaSink);


        // for each entity, read from respective topic and process
        String entities = commonProperties.getProperty("entities");
        String[] entityNames = entities.split(",");

        for (String entityName : entityNames) {
            String entityTopic = getEntityTopic(outTopicPrefix, lob, entityName);
            KafkaSource<StreamingRawData> kafkaSourceEntity = FlinkJobSource.createKafkaSource(inout0Properties, new JsonDeserializationSchema<>(StreamingRawData.class), entityTopic);
            DataStream<StreamingRawData> input = env.fromSource(kafkaSourceEntity, WatermarkStrategy.noWatermarks(), "Kafka source -> " + entityName);
            var processedStream = input
                    .flatMap(new StreamingRawDataFlatMapper())
                    .process(new StreamingRawDataProcessor(commonProperties));
            // add map function to get old record , create and check hash, sink to separate sink to ignore or process further ??
            //        processedStream.sinkTo(new JooqDatabaseBatchSink(outputProperties)).name("Database Success Sink");
            //                .keyBy(t -> t.f0.getTransformerInfo().get(0).getEntityName())


            SingleOutputStreamOperator<EventListenerDTO> batchProcessedStream = processedStream
//                    .keyBy(s->s.f0.getRequestId())
                    .windowAll(GlobalWindows.create())
                    .trigger(CountOrTimeTrigger.of(500, 5000))
 //                   .aggregate(new ListAggregator<>())
 //                   .assignTimestampsAndWatermarks(WatermarkStrategy.<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>>forMonotonousTimestamps()
 //                           .withTimestampAssigner((element, recordTimestamp) -> System.currentTimeMillis()))
                    .process(new BatchSaveProcessor(commonProperties));


            DataStream<StreamingRawData> failedRecords = processedStream.getSideOutput(FAILED_TRANSFORMATIONS);
//            // Create and add the Sink

            String eventTopicName = eventTopic + "-" + lob;
            KafkaTopicCreator.createTopicIfNotExists(eventTopicName, bootstrapServers, 5, (short) 1);

            KafkaSink<EventListenerDTO> eventSink = KafkaSink.<EventListenerDTO>builder()
                    .setBootstrapServers(bootstrapServers)
                    .setRecordSerializer(new EventListenerDTOSerializer(eventTopicName))
                    .build();

            batchProcessedStream.map(event -> {
                String s = JSONUtils.getObjectMapper().writeValueAsString(event);
                LOG.info("Sinking event to Kafka: {} %n {}", event, s);
                return event;
            }).sinkTo(eventSink).name("EventListener Kafka Sink");


            KafkaSink<StreamingRawData> sink = FlinkJobSink.createKafkaSink(inout0Properties, recordKeySerializationSchema, s -> outTopic + "-" + lob);
//            input.sinkTo(sink);
            failedRecords.sinkTo(sink).name("Failed Kafka Sink");
        }
        /* /Note
         * 	Out of order ??
         * 	Key by (entity unique column) for avoiding optimistic errors , but costly process
         * */

        /* /Note
         * 	Window by size or time for batching
         * */

//         var aggregate = input
//                .windowAll(GlobalWindows.create())
//                .trigger(CountOrTimeTrigger.of(100, 5000))
//                .aggregate(new ListAggregator<>())
//                .name("Aggregate Window Count")
//                .process(new StreamingRawDataProcessor(commonProperties))
//                .name("Process Window Count");

        /*
         * Here, you can start creating your execution plan for Flink.
         *
         * Start with getting some data from the environment, like
         * 	env.fromSequence(1, 10);
         *
         * then, transform the resulting DataStream<Long> using operations
         * like
         * 	.filter()
         * 	.flatMap()
         * 	.window()
         * 	.process()
         *
         * and many more.
         * Have a look at the programming guide:
         *
         * https://nightlies.apache.org/flink/flink-docs-stable/
         *
         */

        // Execute program, beginning computation.
        env.execute("Flink Java API Skeleton");
        if (PropertyLoader.isLocal(env)) {
            env.disableOperatorChaining();
        }
    }

    private static String getEntityTopic(String topicPrefix, String lob, String entityName) {
        return String.join("-", topicPrefix, lob, entityName);
    }

}
