package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.salescode.dim.PropertyLoader.mergeProperties;

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
            "\n" +
            "  \"lob\": \"kgbpluat\",\n" +
            "  \"loginId\": \"integration_user\",\n" +
            "  \"batchNumber\": 0,\n" +
            "  \"transformerInfo\": [\n" +
            "    {\n" +
            "      \"skipPreprocessing\": false,\n" +
            "      \"skipPersist\": false,\n" +
            "      \"entityName\": \"Stock\",\n" +
            "      \"transformerId\": \"KgbplStockTransformer\"\n" +
            "\n" +
            "    }\n" +
            "  ],\n" +
            "  \"features\": [\n" +
            "     {\n" +
            "            \"@odata.etag\": \"W/\\\"JzE0NTYzNDU5MDAsNTYzODYyODE5MzswLDA7MzgwMzAzMzcxLDU2Mzc1ODU1ODQn\\\"\",\n" +
            "            \"dataAreaId\": \"kgpl\",\n" +
            "            \"ItemId\": \"KO00010750\",\n" +
            "            \"configId\": \"40\",\n" +
            "            \"InventLocationId\": \"JAPR001\",\n" +
            "            \"InventSiteId\": \"JAPR\",\n" +
            "            \"InventSizeId\": \"\",\n" +
            "            \"InventStyleId\": \"NONPROMO24\",\n" +
            "            \"Acx_ActualBatchId\": \"\",\n" +
            "            \"PdsShelfAdviceDate\": \"1900-01-01T12:00:00Z\",\n" +
            "            \"AvailPhysical\": 0,\n" +
            "            \"ItemName\": \"COKE 750 ML (1X24)\",\n" +
            "            \"InventSerialId\": \"\",\n" +
            "            \"AcxItemType\": \"FinishedGoods\",\n" +
            "            \"InventBatchId\": \"\",\n" +
            "            \"InventColorId\": \"SW\",\n" +
            "            \"ModifiedDate\": \"2024-08-12T09:17:47Z\",\n" +
            "            \"PDSInheritedShelfLife\": \"No\",\n" +
            "            \"expDate\": \"1900-01-01T12:00:00Z\",\n" +
            "            \"prodDate\": \"1900-01-01T12:00:00Z\"\n" +
            "        }\n" +
            "  ],\n" +
            "  \"appId\": \"integration\",\n" +
            "  \"retryCount\": 0,\n" +
            "  \"preserveOnFailure\": true,\n" +
            "  \"ignoreS3Log\": false,\n" +
            "  \"topicName\": \"kgbpldemo-dataintegration\"\n" +
            "}\n";

    @Test
    public void testDataStreamJobWithFewObjects() throws Exception {
        // Clear previously collected values (if any)
        CollectSink.clear();

        // Set up a local Flink streaming environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1); // Simplify testing with one parallel instance

        // Create a few sample StreamingRawData objects
        StreamingRawData data1 = createStreamingDataObject(Map.of("groupId", "req-1"));
//        StreamingRawData data2 = createStreamingDataObject(Map.of("groupId", "req-2"));
//        StreamingRawData data3 = createStreamingDataObject(Map.of("groupId", "req-3"));

        // Create a source from the sample data
        DataStream<StreamingRawData> source = env.fromData(data1);

        // For testing, we bypass Kafka and directly use the processor.
        // Prepare dummy commonProperties (if needed by StreamingRawDataProcessor)
        Map<String, Properties> stringPropertiesMap = PropertyLoader.loadApplicationProperties(null);

        SingleOutputStreamOperator<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> processedStream = AsyncDataStream.unorderedWait(
                source.rebalance().flatMap(new StreamingRawDataFlatMapper()), // Pre-process data
                new StreamingRawDataProcessor(stringPropertiesMap.get("Common")),  // Async Processing
                5, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
        ).process(new ProcessRecordStatus());

        Properties commonProperties = stringPropertiesMap.getOrDefault("Common", new Properties());
        Properties inout0Properties = mergeProperties(stringPropertiesMap.get("InOut0"), commonProperties);
        processedStream.sinkTo(new JooqDatabaseBatchSink(inout0Properties)).name("Database Success Sink");
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
    private StreamingRawData createStreamingDataObject(Map<String, String> map) throws JsonProcessingException {
        return JSONUtils.getObjectMapper()
                .readValue(StringSubstitutor.replace(rawStreamingData, map, "%(", ")"), StreamingRawData.class);
    }

    @Test
    public void testSRD() throws Exception {
        createStreamingDataObject(Map.of("groupId", "req-1"));
    }

}