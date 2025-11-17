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
            "    \"requestId\": \"3e42bc9e-7f91-4f07-8d16-9193352dfd93\",\n" +
            "    \"groupId\": \"ProductDetails_20251117\",\n" +
            "    \"fileId\": null,\n" +
            "    \"lob\": \"cokemrcdemo\",\n" +
            "    \"submittedBy\": null,\n" +
            "    \"transformerInfo\": [\n" +
            "        {\n" +
            "            \"entityName\": \"ProductDetails\",\n" +
            "            \"transformerId\": \"productdetails\",\n" +
            "            \"operationType\": \"insert\",\n" +
            "            \"skipPreprocessing\": \"false\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"topicName\": \"alsafisauat-dataintegration-ProductDetails\",\n" +
            "    \"preserveOnFailure\": true,\n" +
            "    \"features\": [\n" +
            "        {\n" +
            "            \"CATEGORY_CODE\": \"\",\n" +
            "            \"UOM\": \"L\",\n" +
            "            \"SUB_CATEGORY_CODE\": \"Laban\",\n" +
            "            \"SKU_CODE\": \"27/98\",\n" +
            "            \"BRAND_CODE\": \"0845\",\n" +
            "            \"CATEGORY\": \"\",\n" +
            "            \"SUB_CATEGORY\": \"001\",\n" +
            "            \"PACK_SIZE\": \"019\",\n" +
            "            \"PACK_SIZE_DESC\": \"180ml\",\n" +
            "            \"SKU_NAME\": \"2798 Activia Laban 180ml x 6 (1 FREE) FF SL40\",\n" +
            "            \"CONVERSION_UNIT\": \"L\",\n" +
            "            \"BRAND\": \"ACTIVIA\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"loginId\": \"integration_user\",\n" +
            "    \"offset\": null,\n" +
            "    \"retryCount\": null,\n" +
            "    \"ignoreS3Log\": false,\n" +
            "    \"headersMap\": null\n" +
            "}"
            ;
    @Test
    public void testDataStreamJobWithFewObjects() throws Exception {
        // Clear previously collected values (if any)
        CollectSink.clear();

        // Set up a local Flink streaming environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
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
    private StreamingRawData createStreamingDataObject(Map<String, String> map) throws JsonProcessingException {
        return JSONUtils.getObjectMapper()
                        .readValue(StringSubstitutor.replace(rawStreamingData, map, "%(", ")"), StreamingRawData.class);
    }

    @Test
    public void testSRD() throws Exception {
        createStreamingDataObject(Map.of("groupId", "req-1"));
    }

}