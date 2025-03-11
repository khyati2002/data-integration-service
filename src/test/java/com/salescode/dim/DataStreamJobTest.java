package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

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
            "    \"requestId\": \"15efecad-7434-4a8f-8b79-cc6c1b66fcc7\",\n" +
            "    \"groupId\": \"2024-12-19\",\n" +
            "    \"fileId\": null,\n" +
            "    \"lob\": \"ckuatunnati\",\n" +
            "    \"submittedBy\": null,\n" +
            "    \"transformerInfo\": [\n" +
            "        {\n" +
            "            \"entityName\": \"OutletDetails\",\n" +
            "            \"transformerId\": \"unnati_csp_outlet_master_mdm\",\n" +
            "            \"operationType\": \"insert\",\n" +
            "            \"preprocessValidationExcludeGroup\": \"outlet_validation_exclude\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"topicName\": \"flink-test\",\n" +
            "    \"preserveOnFailure\": true,\n" +
            "    \"features\": [\n" +
            "        {\n" +
            "            \"UID\": \"180600161532\",\n" +
            "            \"CREATIONDATE\": \"2024-12-19 02:48:11.067\",\n" +
            "            \"PICKUPDATE\": null,\n" +
            "            \"DISTRICT\": \"NDIS\",\n" +
            "            \"Branch\": \"NDEL\",\n" +
            "            \"CUSTName\": \"NEW INDIA\",\n" +
            "            \"OwnerName\": \"NEW INDIA\",\n" +
            "            \"ChannelType\": \"Town Wholesale\",\n" +
            "            \"OutletType\": \"Pooja Outlet\",\n" +
            "            \"LoyaltyType\": \"SWD Others\",\n" +
            "            \"FoodsTier\": null,\n" +
            "            \"PCPTier\": null,\n" +
            "            \"CustAddress\": \"AICHOR  Greater Noida Uttar Pradesh India\",\n" +
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
            "            \"OutletName\": \"NEW INDIA\",\n" +
            "            \"supplierMapping\": [\n" +
            "                {\n" +
            "                    \"CustID\": \"807\",\n" +
            "                    \"SIFYID\": \"DE5390DMM104807\",\n" +
            "                    \"WDDest\": \"DE5390\",\n" +
            "                    \"UID\": \"180600161532\",\n" +
            "                    \"RCSID\": \"180600161532\",\n" +
            "                    \"WDName\": \"RIDDHI ENTERPRISES\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"loginId\": \"integration_user\",\n" +
            "    \"offset\": null,\n" +
            "    \"retryCount\": null,\n" +
            "    \"ignoreS3Log\": false,\n" +
            "    \"headersMap\": null\n" +
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
        PropertyLoader propertyLoader = new PropertyLoader();
        Map<String, Properties> stringPropertiesMap = propertyLoader.loadApplicationProperties(null);

        // Apply the process function (simulate the job's pipeline)
        DataStream<CommonDataModel> processedStream = source
                // If you had windowing or aggregation, adjust accordingly.
                .process(new StreamingRawDataProcessor(stringPropertiesMap.get("Common")))
                .process(new BatchSaveProcessor(stringPropertiesMap.get("Common")))
                .disableChaining()
                .name("Test Process Function");

        // Add a sink to collect output data
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