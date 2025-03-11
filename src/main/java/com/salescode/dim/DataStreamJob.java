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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.utils.CustomKryoSerializer;
import com.salescode.dim.utils.ImmutableListSerializer;
import com.salescode.dim.utils.KryoConfig;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.api.java.typeutils.runtime.kryo.JavaSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

        env.setParallelism(1);// Prevents Kryo fallback

        env.enableCheckpointing(5000);

        Kryo kryo = KryoConfig.createKryo();
        env.getConfig().enableForceKryo();


        // kryo.register(java.util.List.class, new JavaSerializer());
       // kryo.setDefaultSerializer(JavaSerializer.class);




        PropertyLoader propertyLoader = new PropertyLoader();
        // Load the application properties
        final Map<String, Properties> applicationProperties = propertyLoader.loadApplicationProperties(env);

        LOG.info("Application properties: {}", applicationProperties);

        Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());
        LOG.info(String.valueOf(commonProperties));
        // Prepare the Source and Sink properties
        Properties inputProperties = mergeProperties(applicationProperties.get("Input0"), commonProperties);
        LOG.info(String.valueOf(inputProperties));
        Properties outputProperties = mergeProperties(applicationProperties.get("Output0"), commonProperties);
        LOG.info(String.valueOf(outputProperties));


        KafkaSource<StreamingRawData> source = FlinkJobSource.createKafkaSource(inputProperties, new JsonDeserializationSchema<>(StreamingRawData.class));

        /* /Note
         * 	Out of order ??
         * 	Key by (entity unique column) for avoiding optimistic errors , but costly process
         * */

        DataStream<StreamingRawData> input = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka source");
        LOG.info(input.toString());


        /* /Note
         * 	Window by size or time for batching
         * */

//         var aggregate = input
//                .windowAll(GlobalWindows.create())
//                .trigger(CountOrTimeTrigger.of(100, 5000))
//                .aggregate(new ListAggregator<>())
//                .name("Aggregate Window Count")


//                         .

//        var processedStream = processedData.process(new BatchSaveProcessor(commonProperties));

        var processedStream = input
                .flatMap(new StreamingRawDataFlatMapper())   // Convert raw data to CDMs
                .process(new StreamingRawDataProcessor(commonProperties))  // Process each CDM
                .process(new BatchSaveProcessor(commonProperties))
                .disableChaining()// Perform batch saving
                .name("Batch Save Processor");

        //               .process(new InsertUpdateIgnoreProcessFunction(commonProperties));

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
