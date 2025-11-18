package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.utils.LocalDateTimeKryoSerializer;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A custom sink that collects all elements into a static list for assertions.
 */
class CollectSink<T> implements SinkFunction<T> {
    // Must be static because Flink instantiates the sink in different tasks
    public static final List<Object> values = Collections.synchronizedList(new ArrayList<>());
    private static final long serialVersionUID = -3301416186450647445L;

    public static void clear() {
        values.clear();
    }

    @Override
    public void invoke(T value, Context context) {
        values.add(value);
    }
}

public class DataStreamJobTest {

    public static String rawStreamingData = "{\n" +
            "    \"requestId\": \"c07dcdac-e4c6-4da4-b609-b79fc14fba4f\",\n" +
            "    \"groupId\": \"Asia/Calcutta~2022-01-01 00:00:00~2024-07-15 05:44:00~https://uatxdintegration.cci.vxceed.net/IntegrationService.svc_WarehouseBatchStockC2_100002.xml\",\n" +
            "    \"fileId\": \"4d7a1a5a511269ea4f1e9f2ac6235091\",\n" +
            "    \"lob\": \"kbpluat\",\n" +
            "    \"submittedBy\": \"integration_user\",\n" +
            "    \"transformerInfo\": [\n" +
            "        {\n" +
            "            \"entityName\": \"Stock\",\n" +
            "            \"transformerId\": \"kgpl_stock_transformer_new\",\n" +
            "            \"operationType\": \"insert\",\n" +
            "            \"preprocessValidationExcludeGroup\": \"\",\n" +
            "            \"skipPreprocessing\": \"false\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"topicName\": null,\n" +
            "    \"preserveOnFailure\": true,\n" +
            "    \"features\": [\n" +
            "        {\n" +
            "            \"hierarchycode\": \"_15_N\",\n" +
            "            \"productiondate\": \"2024-01-20T00:00:00\",\n" +
            "            \"itemcode\": \"KW00111001_NONPROMO_15\",\n" +
            "            \"subhierarchycode\": \"\",\n" +
            "            \"mrp\": 15,\n" +
            "            \"itemtypecode\": 1,\n" +
            "            \"expiredate\": \"2025-12-31T00:00:00\",\n" +
            "            \"tenantcode\": 100002,\n" +
            "            \"stockquantity\": 3000,\n" +
            "            \"stockquantity1\": 200\n" +
            "        }\n" +
            "    ],\n" +
            "    \"loginId\": \"applicate\",\n" +
            "    \"offset\": null,\n" +
            "    \"retryCount\": null,\n" +
            "    \"ignoreS3Log\": true,\n" +
            "    \"headersMap\": null\n" +
            "}";

    @Test
    public void testDataStreamJobWithFewObjects() throws Exception {
        // Clear previously collected values (if any)
        CollectSink.clear();

        // Set up a local Flink streaming environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ExecutionConfig config = env.getConfig();
        config.addDefaultKryoSerializer(LocalDateTime.class, new LocalDateTimeKryoSerializer());
        env.setParallelism(1); // Simplify testing with one parallel instance

        // Create a few sample StreamingRawData objects
        StreamingRawData data1 = createStreamingDataObject(Map.of("groupId", "req-1"));
        StreamingRawData data2 = createStreamingDataObject(Map.of("groupId", "req-2"));
        StreamingRawData data3 = createStreamingDataObject(Map.of("groupId", "req-3"));

        // Create a source from the sample data
        DataStream<StreamingRawData> source = env.fromData(data1, data2, data3);

        // For testing, we bypass Kafka and directly use the processor.
        // Prepare dummy commonProperties (if needed by StreamingRawDataProcessor)
        Map<String, Properties> stringPropertiesMap = PropertyLoader.loadApplicationProperties(null);

        SingleOutputStreamOperator<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> processedStream = AsyncDataStream.unorderedWait(
                source.rebalance().flatMap(new StreamingRawDataFlatMapper()), // Pre-process data
                new StreamingRawDataProcessor(stringPropertiesMap.get("Common")),  // Async Processing
                5, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
        ).process(new ProcessRecordStatus());

        processedStream.sinkTo(new JooqDatabaseBatchSink(stringPropertiesMap.get("Common"))).name("Database Success Sink");
        // Execute the pipeline
        env.execute("DataStreamJob Test");

        // Assert that all elements have been processed (order may not be guaranteed)
        List<Object> results = CollectSink.values;
        Assert.assertEquals("Expected 3 elements to be processed", 3, results.size());
        // Further assertions can be made here based on expected processing logic
        for (Object obj : results) {
            System.out.println(obj);
            Assert.assertTrue("Result should be an instance of StreamingRawData", obj instanceof StreamingRawData);
            StreamingRawData streamingRawData = (StreamingRawData) obj;
        }
    }

    @SneakyThrows
    private StreamingRawData createStreamingDataObject(Map<String, String> map) {
        return JSONUtils.getObjectMapper()
                        .readValue(StringSubstitutor.replace(rawStreamingData, map, "%(", ")"), StreamingRawData.class);
    }

    @Test
    public void testSRD() throws Exception {
        createStreamingDataObject(Map.of("groupId", "req-1"));
    }

}