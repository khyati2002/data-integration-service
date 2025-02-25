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

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

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

    private static final Logger LOG = LoggerFactory.getLogger(DataStreamJob.class);

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

//		env.enableCheckpointing(1000);

        // Load the application properties
        final Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(env);

        LOG.info("Application properties: {}", applicationProperties);

        Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());

        // Prepare the Source and Sink properties
        Properties inputProperties = mergeProperties(applicationProperties.get("Input0"), commonProperties);
        Properties outputProperties = mergeProperties(applicationProperties.get("Output0"), commonProperties);

        KafkaSource<StreamingRawData> source = FlinkJobSource.createKafkaSource(inputProperties, new JsonDeserializationSchema<>(StreamingRawData.class));

        /* /Note
         * 	Out of order ??
         * 	Key by (entity unique column) for avoiding optimistic errors , but costly process
         * */

        DataStream<StreamingRawData> input = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka source");

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

        // Process data stream
        var processedStream = input.process(new StreamingRawDataProcessor(commonProperties))
                                   .name("Process StreamingRawData");

        // add map function to get old record , create and check hash, sink to separate sink to ignore or process further ??
//        processedStream.process()

        processedStream.sinkTo(new JooqDatabaseBatchSink(outputProperties)).name("Database Success Sink");


        DataStream<StreamingRawData> failedRecords = processedStream.getSideOutput(FAILED_TRANSFORMATIONS);
        // Create and add the Sink
        KafkaSink<StreamingRawData> sink = FlinkJobSink.createKafkaSink(outputProperties);
        failedRecords.sinkTo(sink).name("Failed Kafka Sink");

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
    }
}
