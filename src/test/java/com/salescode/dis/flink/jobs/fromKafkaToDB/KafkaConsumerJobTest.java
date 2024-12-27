package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import lombok.SneakyThrows;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.flink.util.OutputTag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KafkaConsumerJobTest {

    @ClassRule
    public static final MiniClusterWithClientResource MINI_CLUSTER = new MiniClusterWithClientResource(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(2)
                    .setNumberTaskManagers(1)
                    .build());

    public static final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // Mock dependencies
        DISKafkaSourceBuilder kafkaSourceBuilder = mock(DISKafkaSourceBuilder.class);
        DISKafkaSinkBuilder kafkaSinkBuilder = mock(DISKafkaSinkBuilder.class);

        // Mock Kafka Source
        KafkaSource mockedKafkaSource = mock(KafkaSource.class);
        when(kafkaSourceBuilder.build()).thenReturn(mockedKafkaSource);

    }

    @Test
    public void testExecuteJob() throws Exception {
        // Create a mock execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Create a simple test source
        DataStream<ObjectNode> mockInput = env.fromData(
                createTestObjectNode(a)
        );

        // Simulate processing logic
        OutputTag<String> deadLetterTag = new OutputTag<>("test-dlq") {
        };
        DataStream<CommonDataModel> processedStream = mockInput
                .process(new MessageProcessFunction(deadLetterTag));

        // Mock sinks
        processedStream.addSink(new SinkFunction<>() {
            @Override
            public void invoke(CommonDataModel value, Context context) {
                Assert.assertNotNull(value); // Validate processing
            }
        });

        // Execute the job
        env.execute("Test KafkaConsumerJob");
    }

    @SneakyThrows
    private ObjectNode createTestObjectNode(String json) {
        // Utility to create an ObjectNode for tests
        return mapper.readTree(json).deepCopy();
    }

    String a = "{\n" +
            "    \"groupId\": \"USR000008\",\n" +
            "    \"lob\": \"c\",\n" +
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
}