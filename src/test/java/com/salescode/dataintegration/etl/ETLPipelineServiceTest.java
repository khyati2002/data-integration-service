package com.salescode.dataintegration.etl;

import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.models.enums.EnrichmentPhase;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
import com.salescode.channelkart.enrichments.impl.TestEnrichment;
import com.salescode.channelkart.transformers.impl.TestTransformer;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import com.salescode.dis.config.DatabaseConfig;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.doReturn;

@SpringBootTest(classes = DataIntegrationApplication.class)
@Import(DatabaseConfig.class)
class ETLPipelineServiceTest {

    @Autowired DSLContext dslContext;
    @Autowired ETLPipelineService etlPipelineService;
    @Autowired @SpyBean MetadataRegistry metadataRegistry;
    @Autowired @SpyBean TransformerInfoRegistry transformerInfoRegistry;
    @Autowired @SpyBean EnrichmentInfoRegistry enrichmentInfoRegistry;
    @Autowired @SpyBean ETLRegistry etlRegistry;
    @Autowired EntityUtils entityUtils;

    @BeforeEach
    @SneakyThrows
    void setup(){
//        CkMetadata ckMetadata = new CkMetadata();
//        ckMetadata.setDomainValues(JSONUtils.getObjectMapper().readTree("[{\"dynamicKeys\":[\"outletcode\"]}]"));
//        doReturn(Optional.of(ckMetadata)).when(metadataRegistry).getMetadataByDomainNameAndType("CkOutletDetails", "DynamicUniqueKey");
//        CkTransformerInfo ckTransformerInfo = new CkTransformerInfo();
//        ckTransformerInfo.setType("CkOutletDetails");
//        ckTransformerInfo.setImplementation("com.salescode.dataintegration.etl.impl.TestTransformer");
//        ckTransformerInfo.setActiveStatus(ActiveStatus.ACTIVE);
//        doReturn(ckTransformerInfo).when(transformerInfoRegistry).getTransformerInfoById("testId");
//        CkEnrichmentInfo ckEnrichmentInfo = new CkEnrichmentInfo();
//        ckEnrichmentInfo.setType("CkOutletDetails");
//        ckEnrichmentInfo.setImplementation("com.salescode.dataintegration.etl.impl.TestEnrichment");
//        ckEnrichmentInfo.setActiveStatus(ActiveStatus.ACTIVE);
//        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.PRE_VALIDATION);
//        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.POST_VALIDATION);
//        doReturn(new TestTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.impl.TestTransformer");
//        doReturn(new TestEnrichment()).when(etlRegistry).getEnrichment("com.salescode.dataintegration.etl.impl.TestEnrichment");
    }

    @Test
    void execute() {
        etlPipelineService.execute("{\n" +
                "    \"groupId\": \"USR000008\",\n" +
                "    \"lob\": \"mondelezckinduat\",\n" +
                "    \"transformerInfo\": [\n" +
                "        {\n" +
                "            \"entityName\": \"CkOutletDetails\",\n" +
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