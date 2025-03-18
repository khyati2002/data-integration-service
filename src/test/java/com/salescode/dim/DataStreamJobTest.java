//package com.salescode.dim;
//
//import com.applicate.services.channelkart.models.CommonDataModel;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import lombok.SneakyThrows;
//import org.apache.commons.text.StringSubstitutor;
//import org.apache.flink.api.common.eventtime.WatermarkStrategy;
//import org.apache.flink.configuration.Configuration;
//import org.apache.flink.connector.kafka.sink.KafkaSink;
//import org.apache.flink.connector.kafka.source.KafkaSource;
//import org.apache.flink.formats.json.JsonDeserializationSchema;
//import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
//import org.apache.flink.streaming.api.datastream.DataStream;
//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.flink.streaming.api.functions.sink.SinkFunction;
//import org.apache.flink.test.util.MiniClusterWithClientResource;
//import org.junit.ClassRule;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.util.*;
//import static com.salescode.dim.DataStreamJob.FAILED_TRANSFORMATIONS;
//import static com.salescode.dim.PropertyLoader.mergeProperties;
//import static org.apache.flink.table.functions.BuiltInFunctionDefinitions.LOG;
//
//
///**
// * A custom sink that collects all elements into a static list for assertions.
// */
//class CollectSink<T> implements SinkFunction<T> {
//    // Must be static because Flink instantiates the sink in different tasks
//    public static final List<Object> values = Collections.synchronizedList(new ArrayList<>());
//    private static final long serialVersionUID = -3301416186450647445L;
//
//    public static void clear() {
//        values.clear();
//    }
//
//    private static final Logger LOG = LoggerFactory.getLogger(DataStreamJob.class);
//
//    @Override
//    public void invoke(T value, Context context) {
//        values.add(value);
//    }
//}
//
//public class DataStreamJobTest {
//
//    // setting up a Flink mini cluster as a resource and registers the respective ExecutionEnvironment.
//    @ClassRule
//    public static final MiniClusterWithClientResource MINI_CLUSTER = new MiniClusterWithClientResource(new MiniClusterResourceConfiguration.Builder()
//            .setNumberSlotsPerTaskManager(1)
//            .setNumberTaskManagers(1)
//            .setConfiguration(getConfiguration())
//            .build());
//
//
//    // setting heartbeat configs
//    private static Configuration getConfiguration() {
//        Configuration configuration = new Configuration();
//        // Heartbeat timeouts
//        configuration.setLong("heartbeat.timeout", 600000);  // 5 minutes
//        configuration.setLong("heartbeat.interval", 3 * 20000);  // 60 seconds
//        return configuration;
//    }
//
//    public static String rawStreamingData = "{\"requestId\":\"41267811-c293-4b0d-b10e-f15b56a3ee02\",\"groupId\":\"prod/PromotionMaster_B070_20250223040906_I.zip\",\"fileId\":\"bcf07499e974b988b7652b3c2d2ec01d\",\"lob\":\"kbuddy\",\"submittedBy\":\"integration_user\",\"transformerInfo\":[{\"entityName\":\"SchemeDefination\",\"transformerId\":\"mdm_scheme_hccb\",\"operationType\":\"insert\",\"preprocessValidationExcludeGroup\":\"\",\"skipPreprocessing\":\"false\"}],\"topicName\":null,\"preserveOnFailure\":true,\"features\":[{\"scheme_no\":\"81087326\",\"scheme_line_no\":\"1\",\"distributor_channel\":\"Z1\",\"dist_sap_customer_id\":\"0503996809\",\"mer_wef\":\"2024-06-16 00:00:00\",\"mer_wet\":\"2025-02-22 23:59:59\",\"disbursement_method\":\"1 \",\"disbursement_method_desc\":\"Spot\",\"calculation_method\":\"4 \",\"calculation_method_desc\":\"Free Bottle\",\"monitoring_scope\":\"2 \",\"monitoring_scope_desc\":\"NA\",\"monitoring_uom\":\"EA\",\"monitoring_slab_from\":\"28\",\"monitoring_slab_to\":\"27999\",\"market_scope\":\"3 \",\"market_scope_desc\":\"0503996809\",\"discounted_value\":\"0.07143\",\"discounted_item_id\":\"000000000000103568\",\"discounted_item_uom\":\"EA\",\"external_id\":\"81087326_1_HDZ2_0503996809_G94\",\"isprogresiveslab,\":\"0 \",\"discountedfoctype\":\"1\",\"discountedfoctypedesc\":\"NA\",\"discountedprice\":\"1\",\"exclusion\":\"NA\",\"scheme_desc\":\"BUY 1 to 99 CS GET 2 Bottles free per CS\",\"monitoring_value\":\"000000000000103568\"}],\"loginId\":\"applicate\",\"offset\":null,\"retryCount\":null,\"ignoreS3Log\":true,\"headersMap\":null}";
//    @Test
//    public void testDataStreamJobWithFewObjects() throws Exception {
//        // Clear previously collected values (if any)
//        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//
////		env.enableCheckpointing(1000);
//
//        // Load the application properties
//        final Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(env);
//
////        ("Application properties: {}", applicationProperties);
//
//        // Apply the process function (simulate the job's pipeline)
//        DataStream<CommonDataModel> processedStream = source
//                // If you had windowing or aggregation, adjust accordingly.
////                .process(new StreamingRawDataProcessor(stringPropertiesMap.get("Common")))
////                .process(new BatchSaveProcessor(stringPropertiesMap.get("Common")))
//                .disableChaining()
//                .name("Test Process Function");
//        Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());
//
//        // Prepare the Source and Sink properties
//        Properties inputProperties = mergeProperties(applicationProperties.get("Input0"), commonProperties);
//        Properties outputProperties = mergeProperties(applicationProperties.get("Output0"), commonProperties);
//
//        KafkaSource<StreamingRawData> source = FlinkJobSource.createKafkaSource(inputProperties, new JsonDeserializationSchema<>(StreamingRawData.class));
//
//        /* /Note
//         * 	Out of order ??
//         * 	Key by (entity unique column) for avoiding optimistic errors , but costly process
//         * */
//
//        DataStream<StreamingRawData> input = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka source");
//
//        /* /Note
//         * 	Window by size or time for batching
//         * */
//
////         var aggregate = input
////                .windowAll(GlobalWindows.create())
////                .trigger(CountOrTimeTrigger.of(100, 5000))
////                .aggregate(new ListAggregator<>())
////                .name("Aggregate Window Count")
////                .process(new StreamingRawDataProcessor(commonProperties))
////                .name("Process Window Count");
//
//        // Process data stream
//        var processedStream = input
//                .flatMap(new StreamingRawDataFlatMapper())
//                .process(new StreamingRawDataProcessor(commonProperties));
////                .process(new InsertUpdateIgnoreProcessFunction(commonProperties));
//
//        processedStream.sinkTo(new JooqDatabaseBatchSink(outputProperties)).name("Database Success Sink");
//
//
//        DataStream<StreamingRawData> failedRecords = processedStream.getSideOutput(FAILED_TRANSFORMATIONS);
//        // Create and add the Sink
//        KafkaSink<StreamingRawData> sink = FlinkJobSink.createKafkaSink(outputProperties);
//        failedRecords.sinkTo(sink).name("Failed Kafka Sink");
//
//        /*
//         * Here, you can start creating your execution plan for Flink.
//         *
//         * Start with getting some data from the environment, like
//         * 	env.fromSequence(1, 10);
//         *
//         * then, transform the resulting DataStream<Long> using operations
//         * like
//         * 	.filter()
//         * 	.flatMap()
//         * 	.window()
//         * 	.process()
//         *
//         * and many more.
//         * Have a look at the programming guide:
//         *
//         * https://nightlies.apache.org/flink/flink-docs-stable/
//         *
//         */
//
//        // Execute program, beginning computation.
//        env.execute("Flink Java API Skeleton");
//    }
//
//    @SneakyThrows
//    private StreamingRawData createStreamingDataObject(Map<String, String> map) {
//        return JSONUtils.getObjectMapper()
//                        .readValue(StringSubstitutor.replace(rawStreamingData, map, "%(", ")"), StreamingRawData.class);
//    }
//
//    @Test
//    public void testSRD() throws Exception {
//        createStreamingDataObject(Map.of("groupId", "req-1"));
//    }
//
//}