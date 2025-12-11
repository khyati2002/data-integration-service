package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.api.java.tuple.Tuple2;
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
            "    \"requestId\": \"8a318b6b-95ca-49ff-8310-d90ec102827a\",\n" +
            "    \"groupId\": \"2025-05-02\",\n" +
            "    \"lob\": \"cktestitcloyalty\",\n" +
            "    \"loginId\": \"integration_user\",\n" +
            "    \"batchNumber\": 0,\n" +
            "    \"transformerInfo\": [\n" +
            "        {\n" +
            "            \"skipPreprocessing\": false,\n" +
            "            \"skipPersist\": false,\n" +
            "            \"entityName\": \"OutletDetails\",\n" +
            "            \"transformerId\": \"unnati_csp_outlet_master_mdm1\",\n" +
            "            \"preprocessValidationExcludeGroup\": \"outlet_validation_exclude\",\n" +
            "            \"messageLevelHash\": null,\n" +
            "            \"messageHashSupported\": false,\n" +
            "            \"messageLevelKey\": null,\n" +
            "            \"cachedArtifact\": null,\n" +
            "            \"operationType\": \"insert\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"features\":  [{\n" +
            "    \"UID\": \"EGAU-SL-54327\",\n" +
            "    \"CREATIONDATE\": \"1746770380797\",\n" +
            "    \"DISTRICT\": \"EDIS\",\n" +
            "    \"Branch\": \"EGAU\",\n" +
            "    \"CUSTName\": \"BABUL STORES\",\n" +
            "    \"OwnerName\": \"BABUL STORES\",\n" +
            "    \"ChannelType\": \"Rural Wholesale\",\n" +
            "    \"OutletType\": \"Dual (FMCG + Tobacco)\",\n" +
            "    \"LoyaltyType\": \"SWD Others\",\n" +
            "    \"OutletLat\": \"26.424693999999999\",\n" +
            "    \"OutletLong\": \"90.973511000000002\",\n" +
            "    \"TYPE\": \"LOYALTY\",\n" +
            "    \"OutletName\": \"BABUL STORES\",\n" +
            "    \"supplierMapping\": [\n" +
            "      {\n" +
            "        \"CustID\": \"C651/20-21\",\n" +
            "        \"SIFYID\": \"GA2799DMM333C651/20-21\",\n" +
            "        \"WDDest\": \"GA2799\",\n" +
            "        \"UID\": \"EGAU-SL-54327\",\n" +
            "        \"RCSID\": \"181203463573\",\n" +
            "        \"WDName\": \"HARISH TRADING CO\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"CustID\": \"C651/20-21\",\n" +
            "        \"SIFYID\": \"NG2949DMM333C651/20-21\",\n" +
            "        \"WDDest\": \"NG2949\",\n" +
            "        \"UID\": \"EGAU-SL-54327\",\n" +
            "        \"RCSID\": \"181203463573\",\n" +
            "        \"WDName\": \"HARISH TRADING COOOOOO\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "    ],\n" +
            "    \"appId\": \"integration\",\n" +
            "    \"retryCount\": 0,\n" +
            "    \"preserveOnFailure\": true,\n" +
            "    \"ignoreS3Log\": false,\n" +
            "    \"topicName\": \"unnati-dataintegration\"\n" +
            "}";
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
    private StreamingRawData createStreamingDataObject(Map<String, String> map) {
        return JSONUtils.getObjectMapper()
                        .readValue(StringSubstitutor.replace(rawStreamingData, map, "%(", ")"), StreamingRawData.class);
    }

    @Test
    public void testSRD() throws Exception {
        createStreamingDataObject(Map.of("groupId", "req-1"));
    }

}