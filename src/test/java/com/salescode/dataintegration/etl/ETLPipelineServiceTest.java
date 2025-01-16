package com.salescode.dataintegration.etl;

import com.salescode.DataIntegrationApplication;
import com.salescode.dis.config.DatabaseConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

//@SpringBootTest(classes = DataIntegrationApplication.class)
@Import(DatabaseConfig.class)
@PropertySource(value = "classpath:application.yaml")
class ETLPipelineServiceTest {


    ETLPipelineService etlPipelineService;

    @BeforeEach
    @SneakyThrows
    void setup(){
        ApplicationContext context = SpringApplication.run(DataIntegrationApplication.class);
        etlPipelineService = context.getBean(ETLPipelineService.class);
    }

    @Test
    void execute() {
        etlPipelineService.execute("{\n" +
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
                "            \"UID\": \"1806000027083\",\n" +
                "            \"CREATIONDATE\": \"2024-12-19 02:48:11.067\",\n" +
                "            \"PICKUPDATE\": null,\n" +
                "            \"DISTRICT\": \"SDIS\",\n" +
                "            \"Branch\": \"SHYD\",\n" +
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
                "}");
    }


}