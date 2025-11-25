package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.utils.KryoConfig;
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
                "\t\"requestId\": \"7994c011-16e8-436d-918d-6d5e5ae74281\",\n" +
                "\t\"groupId\": \"DistributorMaster_2025-11-20_12:12:36\",\n" +
                "\t\"fileId\": null,\n" +
                "\t\"lob\": \"cokepheb2b\",\n" +
                "\t\"submittedBy\": null,\n" +
                "\t\"transformerInfo\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"entityName\": \"User\",\n" +
                "\t\t\t\"operationType\": \"insert\",\n" +
                "\t\t\t\"transformerId\": \"CokephDistributorMasterTransformer\",\n" +
                "\t\t\t\"skipPreprocessing\": \"false\"\n" +
                "\t\t}\n" +
                "\t],\n" +
                "\t\"topicName\": null,\n" +
                "\t\"preserveOnFailure\": true,\n" +
                "\t\"features\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"tenant_code\": \"1215\",\n" +
                "\t\t\t\"distributor_code\": \"0504712948\",\n" +
                "\t\t\t\"distributor_name\": \"A PRING'S ENTERPRISES\",\n" +
                "\t\t\t\"distributor_name_a\": \"\",\n" +
                "\t\t\t\"address\": \"DILAB50,\",\n" +
                "\t\t\t\"city\": \"SANTA ROSA,LAGUNA\",\n" +
                "\t\t\t\"state\": \"\",\n" +
                "\t\t\t\"country\": \"PH\",\n" +
                "\t\t\t\"zip\": \"4026\",\n" +
                "\t\t\t\"phone\": \"9288460740\",\n" +
                "\t\t\t\"mobile\": \"+639288460740\",\n" +
                "\t\t\t\"email\": null,\n" +
                "\t\t\t\"website\": \"\",\n" +
                "\t\t\t\"owner_name\": \"PRING, ALVIN JAY T.\",\n" +
                "\t\t\t\"sales_tax_number\": \"448-601-134-000V\",\n" +
                "\t\t\t\"license_number\": \"JBYH\",\n" +
                "\t\t\t\"trade_id_number\": \"\",\n" +
                "\t\t\t\"gst_number\": \"\",\n" +
                "\t\t\t\"pan_card_number\": \"\",\n" +
                "\t\t\t\"category_type\": \"01\",\n" +
                "\t\t\t\"category_code\": \"\",\n" +
                "\t\t\t\"category_desc\": \"\",\n" +
                "\t\t\t\"category_code_1\": \"ES02\",\n" +
                "\t\t\t\"category_description_1\": \"\",\n" +
                "\t\t\t\"category_code_2\": \"46\",\n" +
                "\t\t\t\"category_description_2\": \"\",\n" +
                "\t\t\t\"category_code_3\": \"J\",\n" +
                "\t\t\t\"category_description_3\": \"\",\n" +
                "\t\t\t\"category_code_4\": \"05\",\n" +
                "\t\t\t\"category_description_4\": \"\",\n" +
                "\t\t\t\"category_code_5\": \"48\",\n" +
                "\t\t\t\"category_description_5\": \"\",\n" +
                "\t\t\t\"category_code_6\": \"622\",\n" +
                "\t\t\t\"category_description_6\": \"\",\n" +
                "\t\t\t\"category_code_7\": \"14\",\n" +
                "\t\t\t\"category_description_7\": \"\",\n" +
                "\t\t\t\"category_code_8\": \"037\",\n" +
                "\t\t\t\"category_description_8\": \"\",\n" +
                "\t\t\t\"category_code_9\": \"01\",\n" +
                "\t\t\t\"category_description_9\": \"\",\n" +
                "\t\t\t\"category_code_10\": \"Y022\",\n" +
                "\t\t\t\"category_description_10\": \"\",\n" +
                "\t\t\t\"cfa_code\": \"CCBPI\",\n" +
                "\t\t\t\"territory_hierarchy\": \"0106\",\n" +
                "\t\t\t\"organization_hierarchy\": \"2000155\",\n" +
                "\t\t\t\"currency_code\": \"PHP\",\n" +
                "\t\t\t\"created_date\": \"2015-09-02T00:00:00.000Z\",\n" +
                "\t\t\t\"is_active\": \"1\",\n" +
                "\t\t\t\"parent_distributor_code\": \"\",\n" +
                "\t\t\t\"distributor_attribute_1\": \"48\",\n" +
                "\t\t\t\"geo_code_x\": \"14.308545000000\",\n" +
                "\t\t\t\"geo_code_y\": \"121.110294000000\"\n" +
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
        env.getConfig().registerTypeWithKryoSerializer(LocalDateTime.class, KryoConfig.LocalDateTimeKryoSerializer.class);
        env.setParallelism(1); // Simplify testing with one parallel instance

        // Create a few sample StreamingRawData objects
        StreamingRawData data1 = createStreamingDataObject(Map.of("groupId", "req-1"));
        StreamingRawData data2 = createStreamingDataObject(Map.of("groupId", "req-2"));
        StreamingRawData data3 = createStreamingDataObject(Map.of("groupId", "req-3"));

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