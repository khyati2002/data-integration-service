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
            "  \"requestId\": \"ed5c317c-334ge-b07a-63355ef78cac\",\n" +
            "  \"groupId\": \"2025-11-12\",\n" +
            "  \"fileId\": null,\n" +
            "  \"lob\": \"itcvissfainuat\",\n" +
            "  \"submittedBy\": null,\n" +
            "  \"transformerInfo\": [\n" +
            "    {\n" +
            "      \"entityName\": \"ProductMetadata\",\n" +
            "      \"transformerId\": \"mdm_productmetadata_java_integ\",\n" +
            "      \"operationType\": \"insert\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"topicName\": null,\n" +
            "  \"preserveOnFailure\": true,\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"WDDEST\": \"CA3704\",\n" +
            "      \"SubCatCode\": \"AMNPS\",\n" +
            "      \"SubCatName\": \"Popular Small Pack\",\n" +
            "      \"SBU\": \"AGARBATTI\",\n" +
            "      \"MktSkuCode\": \"MD20MOGRA\",\n" +
            "      \"MktSkuName\": \"MD20MOGRA\",\n" +
            "      \"SysSkuCode\": \"11590\",\n" +
            "      \"SysSkuName\": \"Mangaldeep 12 Mogra(MB)\",\n" +
            "      \"SimpleProductDesc\": \"MD Mogra Agarbatti - Rs 10\",\n" +
            "      \"CatCode\": \"AG\",\n" +
            "      \"CatName\": \"AGARBATTI\",\n" +
            "      \"BrandCode\": \"MN\",\n" +
            "      \"BrandName\": \"MANGALDEEP\",\n" +
            "      \"Tax\": \"5.000000\",\n" +
            "      \"PACPTR\": \"10.333400\",\n" +
            "      \"CFCPTR\": \"2005.004600\",\n" +
            "      \"PACIN1CFC\": 240,\n" +
            "      \"MRP\": \"45.000000\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"loginId\": \"integration_user\",\n" +
            "  \"offset\": null,\n" +
            "  \"retryCount\": null,\n" +
            "  \"ignoreS3Log\": false,\n" +
            "  \"headersMap\": null\n" +
            "}";

//    public static String rawStreamingData = "{\n" +
//            "   \"requestId\": \"782cc28b-f246-4907-8637-cf51ae61139f\",\n" +
//            "   \"groupId\": \"2025-11-10\",\n" +
//            "   \"fileId\": null,\n" +
//            "   \"lob\": \"itcvissfain\",\n" +
//            "   \"submittedBy\": null,\n" +
//            "   \"transformerInfo\": [\n" +
//            "       {\n" +
//            "           \"entityName\": \"OutletDetails\",\n" +
//            "           \"transformerId\": \"mdm_stockist_transformer_integ1\",\n" +
//            "           \"operationType\": \"insert\"\n" +
//            "       }\n" +
//            "   ],\n" +
//            "   \"topicName\": null,\n" +
//            "   \"preserveOnFailure\": true,\n" +
//            "   \"features\": [\n" +
//            "       {\n" +
//            "           \"District\": \"EDIS\",\n" +
//            "           \"Branch\": \"EBEN\",\n" +
//            "           \"PSRCRMID\": \"4001170\",\n" +
//            "           \"stockistName\": \"EAHAN STORE\",\n" +
//            "           \"userName\": null,\n" +
//            "           \"KYC\": \"Y\",\n" +
//            "           \"STOCKIST_L3M_BAND\": \"PLATINUM\",\n" +
//            "           \"UID\": \"C20220005437260\",\n" +
//            "           \"AUS\": \"\",\n" +
//            "           \"ActiveStatus\": \"ACTIVE\",\n" +
//            "           \"StockistBand\": \"PLATINUM\",\n" +
//            "           \"CREATION_DATE\": \"2025-11-10\",\n" +
//            "           \"PICK_UP_STATUS\": \"0\",\n" +
//            "           \"LOADDATE\": \"2025-11-10\",\n" +
//            "           \"BeatID\": null,\n" +
//            "           \"Beat\": null,\n" +
//            "           \"COLOUR\": null,\n" +
//            "           \"supplierMapping\": [\n" +
//            "               {\n" +
//            "                   \"CustID\": \"342EXP2023\",\n" +
//            "                   \"SIFYID\": \"CA3210ITC510342EXP2023\",\n" +
//            "                   \"WDDest\": \"CA3210\",\n" +
//            "                   \"UID\": \"C20220005437260\",\n" +
//            "                   \"RCSID\": \"181204528215\",\n" +
//            "                   \"WDName\": \" EAHAN STORE\"\n" +
//            "               }\n" +
//            "           ],\n" +
//            "           \"WDDest\": [\n" +
//            "               \"CA3210\"\n" +
//            "           ]\n" +
//            "       }\n" +
//            "   ],\n" +
//            "   \"loginId\": \"integration_user\",\n" +
//            "   \"offset\": null,\n" +
//            "   \"retryCount\": null,\n" +
//            "   \"ignoreS3Log\": false,\n" +
//            "   \"headersMap\": null\n" +
//            "}";
//    public static String rawStreamingData = "{\n" +
//            "    \"requestId\": \"99deb-ererfe543223vsdvf=3e-8581-9vfd70r21232\",\n" +
//            "    \"groupId\": \"2025-03-22\",\n" +
//            "    \"lob\": \"itcvissfainuat\",\n" +
//            "    \"loginId\": \"admin@applicate.in\",\n" +
//            "    \"batchNumber\": 0,\n" +
//            "    \"transformerInfo\": [\n" +
//            "        {\n" +
//            "            \"skipPreprocessing\": false,\n" +
//            "            \"skipPersist\": false,\n" +
//            "            \"entityName\": \"User\",\n" +
//            "            \"transformerId\": \"mdm_user_psr_integ_test\",\n" +
//            "            \"preprocessValidationExcludeGroup\": null,\n" +
//            "            \"messageLevelHash\": \"NUgKG5wpO4h0jfQJoXzBVUUVfX2sPWej4PXSdlD7HRw=\",\n" +
//            "            \"messageHashSupported\": false,\n" +
//            "            \"messageLevelKey\": \"mdm_user_psr_integ:User:4002583\",\n" +
//            "            \"cachedArtifact\": null,\n" +
//            "            \"operationType\": \"insert\"\n" +
//            "        }\n" +
//            "    ],\n" +
//            "    \"features\": [\n" +
//            "        {\n" +
//            "            \"District\": \"EDIS\",\n" +
//            "            \"Branch\": \"EGAU\",\n" +
//            "            \"immediateParent\": \"NG22411\",\n" +
//            "            \"PSRCRMID\": \"6842336\",\n" +
//            "            \"PSRName\": \"Samarth hcPSR\",\n" +
//            "            \"userName\": \"6112021545\",\n" +
//            "            \"DSType\": \"PSR\",\n" +
//            "            \"AUS\": \"Y\",\n" +
//            "            \"PICK_UP_STATUS\": \"0\",\n" +
//            "            \"source_key\": \"integration\"\n" +
//            "        }\n" +
//            "    ]\n" +
//            "}";

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
        //DataStream<StreamingRawData> source = env.fromData(data1, data2, data3);
        DataStream<StreamingRawData> source = env.fromData(data1);

        // For testing, we bypass Kafka and directly use the processor.
        // Prepare dummy commonProperties (if needed by StreamingRawDataProcessor)
        Map<String, Properties> stringPropertiesMap = PropertyLoader.loadApplicationProperties(null);

        SingleOutputStreamOperator<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> processedStream = AsyncDataStream.unorderedWait(
                source.rebalance().flatMap(new StreamingRawDataFlatMapper()), // Pre-process data
                new StreamingRawDataProcessor(stringPropertiesMap.get("Common")),  // Async Processing
                5000, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
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