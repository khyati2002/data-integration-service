package com.salescode.dis.flink.jobs.fromKafkaToDB;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dis.config.DatabaseConfig;
import com.salescode.dis.flink.aggregator.ListAggregator;
import com.salescode.dis.flink.sinks.DISKafkaSinkBuilder;
import com.salescode.dis.flink.sources.DISKafkaSourceBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.flink.util.OutputTag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(DatabaseConfig.class)
@PropertySource(value = "classpath:application.yaml")
@Slf4j
public class KafkaConsumerJobTest {

    @ClassRule
    public static final MiniClusterWithClientResource MINI_CLUSTER = new MiniClusterWithClientResource(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(1)
                    .setNumberTaskManagers(1)
                    .setConfiguration(getConfiguration())
                    .build());

    private static Configuration getConfiguration() {
        Configuration configuration = new Configuration();
        // Heartbeat timeouts
        configuration.setLong("heartbeat.timeout", 600000);  // 5 minutes
        configuration.setLong("heartbeat.interval", 3 * 20000);  // 60 seconds
        return configuration;
    }
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
        List<ObjectNode> nodes = new ArrayList<>();
        for(int i = 0;i<1;i++){
            nodes.add(createTestObjectNode(sampleJson.replaceAll("180600002708","180600002708"+i).replace("e94d4d3446ea","e94d4d3446ea="+i)));
        }
        DataStream<ObjectNode> mockInput = env.fromElements(nodes.toArray(new ObjectNode[nodes.size()]));

        OutputTag<String> deadLetterTag = new OutputTag<>("test-dlq"){};

        SingleOutputStreamOperator<List<CommonDataModel>> processedStream = mockInput
//                .windowAll(TumblingProcessingTimeWindows.of(Time.milliseconds(8)))
                .countWindowAll(1)
                .aggregate(new ListAggregator<ObjectNode>())
                .map(new MapFunction<List<ObjectNode>, List<ObjectNode>>() {
                    @Override
                    public List<ObjectNode> map(List<ObjectNode> s) throws Exception {
                        log.info("Batch size processing {}", s.size());
                        return (List<ObjectNode>) s;
                    }
                })
                .process(new MessageProcessFunction(deadLetterTag));

        processedStream.addSink(new TestSink());

        DataStream<String> deadLetterStream = processedStream.getSideOutput(deadLetterTag);
        deadLetterStream.addSink(new TestDLQSink());

        env.execute("Test KafkaConsumerJob");
    }

    private static class TestSink implements SinkFunction<List<CommonDataModel>> {
        @Override
        public void invoke(List<CommonDataModel> value, Context context) {
            Assert.assertNotNull(value);
            Assert.assertFalse(value.isEmpty());
        }
    }

    private static class TestDLQSink implements SinkFunction<String> {
        @Override
        public void invoke(String value, Context context) {
            Assert.assertNotNull(value);
        }
    }

    @SneakyThrows
    private ObjectNode createTestObjectNode(String json) {
        return (ObjectNode) mapper.readTree(json);
    }


    String sampleJson = "{\n" +
            "    \"requestId\": \"a6d6e1f1-bb5f-460a-82bf-e94d4d3446ea\",\n" +
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
            "            \"UID\": \"180600002708\",\n" +
            "            \"CREATIONDATE\": \"2024-12-19 02:48:11.067\",\n" +
            "            \"PICKUPDATE\": null,\n" +
            "            \"DISTRICT\": \"EDIS\",\n" +
            "            \"Branch\": \"ECAL\",\n" +
            "            \"CUSTName\": \"SAI VENKATESWARA K/G (B/S SHIVA SAI)\",\n" +
            "            \"OwnerName\": \"SAI VENKATESWARA K/G (B/S SHIVA SAI)\",\n" +
            "            \"ChannelType\": \"Retail\",\n" +
            "            \"OutletType\": \"Grocery\",\n" +
            "            \"LoyaltyType\": \"Retail Class B\",\n" +
            "            \"FoodsTier\": null,\n" +
            "            \"PCPTier\": null,\n" +
            "            \"CustAddress\": \"# 8-4-36/J/275 Site-3 NRR Puram - 988572945 # 8-4-\",\n" +
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
            "            \"OutletName\": \"SAI VENKATESWARA K/G (B/S SHIVA SAI)\",\n" +
            "            \"supplierMapping\": [\n" +
            "                {\n" +
            "                    \"CustID\": \"IMP092401721\",\n" +
            "                    \"SIFYID\": \"HY382410TRS129IMP092401721\",\n" +
            "                    \"WDDest\": \"HY382410\",\n" +
            "                    \"UID\": \"180600002708\",\n" +
            "                    \"RCSID\": \"180600002708\",\n" +
            "                    \"WDName\": \"KUMARVELU BROTHERS\"\n" +
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
}
