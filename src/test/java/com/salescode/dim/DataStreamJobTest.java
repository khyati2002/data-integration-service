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
            "\t\"requestId\": \"e7512bb1-1619-4ea8-9c6e-155855f75dc9\",\n" +
            "\t\"groupId\": \"OutletDetails_2025-11-14_13:37:17\",\n" +
            "\t\"fileId\": null,\n" +
            "\t\"lob\": \"cokephuat\",\n" +
            "\t\"submittedBy\": null,\n" +
            "\t\"transformerInfo\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"entityName\": \"OutletDetails\",\n" +
            "\t\t\t\"operationType\": \"insert\",\n" +
            "\t\t\t\"transformerId\": \"CokephOutletDetailsTransformer\",\n" +
            "\t\t\t\"skipPreprocessing\": \"false\"\n" +
            "\t\t}\n" +
            "\t],\n" +
            "\t\"topicName\": null,\n" +
            "\t\"preserveOnFailure\": true,\n" +
            "\t\"features\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"tenant_code\": \"1215\",\n" +
            "\t\t\t\"outlet_code\": \"0505380464\",\n" +
            "\t\t\t\"outlet_name\": \"JAMS EATERY\",\n" +
            "\t\t\t\"address_1\": \"SAN NICOLAS BINONDO\",\n" +
            "\t\t\t\"address_2\": \"312\",\n" +
            "\t\t\t\"address_3\": \"MD SANTOS\",\n" +
            "\t\t\t\"city\": \"BINONDO,MANILA\",\n" +
            "\t\t\t\"zip\": \"1006\",\n" +
            "\t\t\t\"mobile\": \"+6393066601167\",\n" +
            "\t\t\t\"phone\": \"093066601167\",\n" +
            "\t\t\t\"email\": null,\n" +
            "\t\t\t\"contact_person\": \"AGNES\",\n" +
            "\t\t\t\"category_code_1\": \"AAFN\",\n" +
            "\t\t\t\"category_code_2\": \"46\",\n" +
            "\t\t\t\"category_code_3\": \"A\",\n" +
            "\t\t\t\"category_code_4\": \"07\",\n" +
            "\t\t\t\"category_code_5\": \"48\",\n" +
            "\t\t\t\"category_code_6\": \"521\",\n" +
            "\t\t\t\"category_code_7\": \"14\",\n" +
            "\t\t\t\"category_code_8\": \"011\",\n" +
            "\t\t\t\"category_code_9\": \"01\",\n" +
            "\t\t\t\"territory_hierarchy\": \"0100\",\n" +
            "\t\t\t\"geo_code_x\": \"14.601118000000\",\n" +
            "\t\t\t\"geo_code_Y\": \"120.967717000000\",\n" +
            "\t\t\t\"outlet_status\": \"1\",\n" +
            "\t\t\t\"sales_mode\": \"1\",\n" +
            "\t\t\t\"payment_type\": \"1\",\n" +
            "\t\t\t\"is_taxable\": \"1\",\n" +
            "\t\t\t\"owner_name\": \"FAUSTINO, AGNES M.\",\n" +
            "\t\t\t\"date_of_registration\": \"2023-01-20T00:07:00.000Z\",\n" +
            "\t\t\t\"tin_number\": \"000-000-000-000V\",\n" +
            "\t\t\t\"distributor_outlet_mapping\": [\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\t\"tenant_code\": \"1215\",\n" +
            "\t\t\t\t\t\"outlet_code\": \"0505380464\",\n" +
            "\t\t\t\t\t\"is_active\": \"1\",\n" +
            "\t\t\t\t\t\"distributor_code\": \"0505372148\",\n" +
            "\t\t\t\t\t\"distributor_outlet_code\": \"0505380464\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t],\n" +
            "\t\t\t\"cb_code\": \"AEYI\"\n" +
            "\t\t}\n" +
            "\t],\n" +
            "\t\"loginId\": \"integration_user\",\n" +
            "\t\"offset\": null,\n" +
            "\t\"retryCount\": null,\n" +
            "\t\"ignoreS3Log\": false,\n" +
            "\t\"headersMap\": null\n" +
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