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
            "  \"requestId\": \"8a318b6b-95ca-49ff-8310-d90ec102827a\",\n" +
            "  \"groupId\": \"%(groupId)\",\n" +
            "  \"lob\": \"cokesauat\",\n" +
            "  \"loginId\": \"integration_user\",\n" +
            "  \"batchNumber\": 0,\n" +
            "  \"transformerInfo\": [\n" +
            "    {\n" +
            "      \"skipPreprocessing\": false,\n" +
            "      \"skipPersist\": false,\n" +
            "      \"entityName\": \"VanLoadout\",\n" +
            "      \"transformerId\": \"VanLoadoutTransformerCokeSA\",\n" +
            "      \"preprocessValidationExcludeGroup\": null,\n" +
            "      \"messageLevelHash\": null,\n" +
            "      \"messageHashSupported\": false,\n" +
            "      \"messageLevelKey\": null,\n" +
            "      \"cachedArtifact\": null,\n" +
            "      \"operationType\": \"insert\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"dmsVanLoadout\": {\n" +
            "        \"loadNumber\": \"LD_30_1251113_RY454_309127\",\n" +
//            "        \"loadOutStatus\": \"IN_TRANSIT\",\n" +
            "        \"salesmanId\": \"SM001\",\n" +
            "        \"supplier\": \"SUPPLIER_A\",\n" +
            "        \"totalCaseQty\": 150,\n" +
            "        \"totalCaseLeftQty\": 150,\n" +
            "        \"routeCode\": \"RY454\" ,\n" +
//        "        \"totalPieceQty\": 500,\n" +
//        "        \"totalPieceLeftQty\": 500,\n" +
//        "        \"totalOtherQty\": 50,\n" +
//        "        \"totalOtherLeftQty\": 50,\n" +
            "        \"vehicleId\": \"VEH002\"\n" +
//        "        \"vehicleCapacity\": 2000,\n" +
//        "        \"loadOutDate\": \"2025-01-15 08:00:00\",\n" +
//        "        \"loadoutSource\": \"ORDER\",\n" +
//        "        \"caseShortage\": 0,\n" +
//        "        \"pieceShortage\": 0,\n" +
//        "        \"otherShortage\": 0,\n" +
//        "        \"deliveryStartDate\": \"2025-01-15 08:00:00\",\n" +
//        "        \"deliveryEndDate\": \"2025-01-15 18:00:00\",\n" +
//        "        \"settlementDate\": \"2025-01-19 23:59:59\",\n" +
//        "        \"shortageUpdated\": 0,\n" +
//        "        \"invoiceCreationStartDate\": \"2025-01-15 09:00:00\",\n" +
//        "        \"invoiceCreationEndDate\": \"2025-01-15 17:00:00\"\n" +
            "      },\n" +
            "      \"vanItemsList\": [\n" +
            "        {\n" +
            "          \"skuCode\": \"SKU001\",\n" +
//          "          \"batchCode\": \"BATCH001\",\n" +
//          "          \"batchId\": \"B001\",\n" +
            "          \"caseQty\": 30,\n" +
            "          \"caseQtyLeft\": 30\n" +
//          "          \"pieceQty\": 100,\n" +
//          "          \"pieceQtyLeft\": 100,\n" +
//          "          \"otherQty\": 10,\n" +
//          "          \"otherQtyLeft\": 10,\n" +
//            "          \"itemType\": \"NORMAL\"\n" +
//            "          \"loadNumber\": \"LD_30_1251113_RY454_309127\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"skuCode\": \"SKU002\",\n" +
//          "          \"batchCode\": \"BATCH002\",\n" +
//          "          \"batchId\": \"B002\",\n" +
            "          \"caseQty\": 40,\n" +
            "          \"caseQtyLeft\": 40\n" +
//          "          \"pieceQty\": 150,\n" +
//          "          \"pieceQtyLeft\": 150,\n" +
//          "          \"otherQty\": 15,\n" +
//          "          \"otherQtyLeft\": 15,\n" +
//            "          \"itemType\": \"NORMAL\"\n" +
//            "          \"loadNumber\": \"LD_30_1251113_RY454_309127\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"status\": \"Failure\",\n" +
            "  \"appId\": \"integration\",\n" +
            "  \"retryCount\": 0,\n" +
            "  \"preserveOnFailure\": true,\n" +
            "  \"ignoreS3Log\": false,\n" +
            "  \"topicName\": \"cokesauat-dataintegration-VanLoadout\"\n" +
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
                50000, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
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