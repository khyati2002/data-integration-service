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

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.TopicSelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.salescode.dim.PropertyLoader.isLocal;
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
@Slf4j
public class DataStreamJob {

    // Define SideOutputTag for failed records
    public static final OutputTag<StreamingRawData> FAILED_TRANSFORMATIONS = new OutputTag<>("failed-transformations") {};

    public static final SerializationSchema<StreamingRawData> recordKeySerializationSchema = (StreamingRawData element) -> element.getRequestId().getBytes();



    private static final Logger LOG = LoggerFactory.getLogger(DataStreamJob.class);

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        Configuration cfg = new Configuration();
        cfg.set(RestOptions.PORT, 8085);
        StreamExecutionEnvironment baseEnv = StreamExecutionEnvironment.getExecutionEnvironment();
        final StreamExecutionEnvironment env = isLocal(baseEnv) ? StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(cfg) : baseEnv;

        // Load the application properties
        final Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(env);
        LOG.info("Application properties: {}", applicationProperties);

        Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());
        ConfigValidator.validate(commonProperties, "lob", "bootstrapServers", "entities");

        String bootstrapServers = commonProperties.getProperty("bootstrap.servers").trim();
        String lob = commonProperties.getProperty("lob").trim();
        String entities = commonProperties.getProperty("entities").trim();
        String[] entityNames = entities.split(",");

        Properties inout0Properties = mergeProperties(applicationProperties.get("InOut0"), commonProperties);
        ConfigValidator.validate(inout0Properties, "input.topic", "failure.topic");

        String inputTopicPostFix = inout0Properties.getProperty("input.topic").trim();
        String failureTopicPostFix = inout0Properties.getProperty("failure.topic").trim();
        String eventTopicPostFix = inout0Properties.getProperty("event.topic", "event").trim();

        String lobTopic = String.join("-", lob, inputTopicPostFix);           // cktestitcloyalty-dataintegration
        String lobFailureTopic = String.join("-", lob, failureTopicPostFix);  // cktestitcloyalty-dataintegration-failure or cktestitcloyalty-int-failure-streams
        String lobEventTopic = String.join("-", lob, eventTopicPostFix);      // cktestitcloyalty-dataintegration-event
        String lobOutTopic = String.join("-", lobTopic, "out");     // cktestitcloyalty-dataintegration-out (for testing only)

        // Create lob topics if not exists
        KafkaTopicCreator.createTopicIfNotExists(lobTopic, bootstrapServers);
        KafkaTopicCreator.createTopicIfNotExists(lobFailureTopic, bootstrapServers);
        KafkaTopicCreator.createTopicIfNotExists(lobEventTopic, bootstrapServers);
        if(isLocal(env)) {
            KafkaTopicCreator.clearAndRecreateTopic(lobOutTopic, bootstrapServers);
        }

        Map<String, String> entityTopicMap = Arrays.stream(entityNames).distinct().parallel()
                                            .map(entityName -> Map.entry(entityName, String.join("-", lobTopic, entityName)))
                                            .peek(lobEntityTopic -> createEntityTopic(lobEntityTopic, env, bootstrapServers))
                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Read from kafka and bifurcate on basis of entities and sink to respective topics
        KafkaSource<StreamingRawData> kafkaSource = FlinkJobSource.createKafkaSource(inout0Properties, new JsonDeserializationSchema<>(StreamingRawData.class), lobTopic);

        TopicSelector<StreamingRawData> topicSelector = (StreamingRawData record) -> Optional.ofNullable(record.getTransformerInfo())
                                                                                             .filter(s -> !s.isEmpty())
                                                                                             .map(t -> t.get(0).getEntityName()) // we will process all entities in TransformerList in transformation function
                                                                                             .filter(entity -> !entity.isEmpty() && entityTopicMap.containsKey(entity))
                                                                                             .map(entityTopicMap::get)
                                                                                             .orElseGet(() -> {
                                                                                                 record.setResponses(List.of(new StreamingRawData.Response("Failure", "Entity not found")));
                                                                                                 return lobFailureTopic;
                                                                                             });

        KafkaSink<StreamingRawData> kafkaSink = FlinkJobSink.createKafkaSink(inout0Properties, recordKeySerializationSchema, topicSelector);

        env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka source").name("Entity Bifurcation")
           .sinkTo(kafkaSink);

        // for each entity, read from respective topic and process
        for (String entityName : entityNames) {
            String entityTopic = entityTopicMap.get(entityName);
            KafkaSource<StreamingRawData> kafkaSourceEntity = FlinkJobSource.createKafkaSource(inout0Properties, new JsonDeserializationSchema<>(StreamingRawData.class), entityTopic);
            DataStream<StreamingRawData> input = env.fromSource(kafkaSourceEntity, WatermarkStrategy.noWatermarks(), "Entity Kafka source" + entityName).name(entityName + "-Source");
            var processedStream = AsyncDataStream.unorderedWait(
                            input.rebalance().flatMap(new StreamingRawDataFlatMapper()), // Pre-process data
                            new StreamingRawDataProcessor(commonProperties),  // Async Processing
                            5, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
                    ).process(new ProcessRecordStatus());

           processedStream.sinkTo(new JooqDatabaseBatchSink(inout0Properties)).name("Database Success Sink");

           // Failed records
            DataStream<StreamingRawData> failedRecords = processedStream.getSideOutput(FAILED_TRANSFORMATIONS);
            KafkaSink<StreamingRawData> sink = FlinkJobSink.createKafkaSink(inout0Properties, recordKeySerializationSchema, s -> lobFailureTopic);
            failedRecords.sinkTo(sink).name("Failed Kafka Sink");
        }
        /* /Note
         * 	Out of order ??
         * 	Key by (entity unique column) for avoiding optimistic errors , but costly process
         * 	Window by size or time for batching
         * */

        env.execute("Flink Java API Skeleton");
    }

    private static void createEntityTopic(Map.Entry<String, String> lobEntityTopic, StreamExecutionEnvironment env, String bootstrapServers) {
        if (isLocal(env)) {
            KafkaTopicCreator.clearAndRecreateTopic(lobEntityTopic.getValue(), bootstrapServers);
        } else {
            KafkaTopicCreator.createTopicIfNotExists(lobEntityTopic.getValue(), bootstrapServers);
        }
    }

}
