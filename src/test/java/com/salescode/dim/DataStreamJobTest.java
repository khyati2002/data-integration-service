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
            "    \"groupId\": \"%(groupId)\",\n" +
            "    \"lob\": \"mondelezckinduat\",\n" +
            "    \"transformerInfo\": [\n" +
            "        {\n" +
            "            \"entityName\": \"OutletDetails\",\n" +
            "            \"operationType\": \"insert\",\n" +
            "            \"transformerId\": \"unnati_csp_outlet_master_mdm\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"topicName\": \"flink-test\",\n" +
            "    \"preserveOnFailure\": true,\n" +
            "    \"features\": [\n" +
            "        {\n" +
            "            \"UID\": \"C20220005809717\",\n" +
            "            \"CREATIONDATE\": \"2024-06-10 04:08:01.873\",\n" +
            "            \"PICKUPDATE\": null,\n" +
            "            \"DISTRICT\": \"EDIS\",\n" +
            "            \"Branch\": \"EVIZ\",\n" +
            "            \"CUSTName\": \"VISHAKA PALOUR\",\n" +
            "            \"OwnerName\": \"VISHAKA PALOUR\",\n" +
            "            \"ChannelType\": \"Retail\",\n" +
            "            \"OutletType\": \"Convenience Outlet\",\n" +
            "            \"LoyaltyType\": \"Retail Others\",\n" +
            "            \"FoodsTier\": null,\n" +
            "            \"PCPTier\": null,\n" +
            "            \"CustAddress\": \"KARANAM GARI JN\",\n" +
            "            \"CustState\": null,\n" +
            "            \"CustCity\": null,\n" +
            "            \"PIN\": null,\n" +
            "            \"Mobile\": null,\n" +
            "            \"BirthDate\": null,\n" +
            "            \"Anniversary\": null,\n" +
            "            \"PCPSubType\": null,\n" +
            "            \"FCFoodsSubType\": null,\n" +
            "            \"ITCProducts\": \"Y\",\n" +
            "            \"GiftVoucher\": \"Y\",\n" +
            "            \"OutletLat\": null,\n" +
            "            \"OutletLong\": null,\n" +
            "            \"CustOrder\": \"Y\",\n" +
            "            \"CustLoyalty\": \"N\",\n" +
            "            \"AutoRedemption\": \"Y\",\n" +
            "            \"Active\": \"Y\",\n" +
            "            \"TYPE\": \"non loyalty\",\n" +
            "            \"OutletName\": \"VISHAKA PALOUR\",\n" +
            "            \"supplierMapping\": [\n" +
            "                {\n" +
            "                    \"CustID\": \"UK029\",\n" +
            "                    \"SIFYID\": \"VI3493CIS722UK029\",\n" +
            "                    \"WDDest\": \"VI3493\",\n" +
            "                    \"UID\": \"C20220005809717\",\n" +
            "                    \"RCSID\": \"181204899725\",\n" +
            "                    \"WDName\": \"SRI DEVAKI LOGISTICS\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
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

        // Apply the process function (simulate the job's pipeline)
        SingleOutputStreamOperator<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> processedStream = AsyncDataStream.unorderedWait(
                source.rebalance().flatMap(new StreamingRawDataFlatMapper()), // Pre-process data
                new StreamingRawDataProcessor(stringPropertiesMap.get("Common")),  // Async Processing
                5, TimeUnit.SECONDS  // Timeout to prevent blocking indefinitely
        ).process(new ProcessRecordStatus());
        // Add a sink to collect output data

        processedStream.sinkTo(new JooqDatabaseBatchSink(stringPropertiesMap.get("Common"))).name("Database Success Sink");
        processedStream.addSink(new CollectSink<>());

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