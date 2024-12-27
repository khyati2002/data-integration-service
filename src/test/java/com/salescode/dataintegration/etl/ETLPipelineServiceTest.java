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
                "    \"groupId\": \"USR000008\",\n" +
                "    \"lob\": \"ckuatunnati\",\n" +
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
                "}");
    }


}