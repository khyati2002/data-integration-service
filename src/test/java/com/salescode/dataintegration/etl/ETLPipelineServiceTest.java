package com.salescode.dataintegration.etl;

import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
import com.salescode.dataintegration.etl.impl.GenericOutletDetailsEnrichment;
import com.salescode.dataintegration.etl.impl.OutletDetailsNameEnrichmentITCL;
import com.salescode.dataintegration.etl.impl.TestEnrichment;
import com.salescode.dataintegration.etl.impl.TestTransformer;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import com.salescode.dis.FlinkApplication;
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
//        ckEnrichmentInfo.setImplementation("com.salescode.dataintegration.bundle.OutletDetailsNameEnrichmentITCL");
//        ckEnrichmentInfo.setActiveStatus(ActiveStatus.ACTIVE);
//        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.PRE_VALIDATION);
//        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.POST_VALIDATION);
//        doReturn(new TestTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.impl.TestTransformer");
//        doReturn(new OutletDetailsNameEnrichmentITCL()).when(etlRegistry).getEnrichment("com.salescode.dataintegration.bundle.OutletDetailsNameEnrichmentITCL");

    }

    @Test
    void execute() {
        etlPipelineService.execute(" {\n" +
                "                    \"groupId\": \"USR000008\",\n" +
                "                    \"lob\": \"mondelezckinduat\",\n" +
                "                    \"transformerInfo\": [\n" +
                "                        {\n" +
                "                            \"entityName\": \"CkOutletDetails\",\n" +
                "                            \"operationType\": \"insert\",\n" +
                "                            \"transformerId\": \"testId\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"preserveOnFailure\": true,\n" +
                "                    \"features\": [\n" +
                "                         {\n" +
                        "\t\"address\": \"KARANAM GARI JN\",\n" +
                        "\t\"doo\": null,\n" +
                        "\t\"immediateParent\": [\n" +
                        "\t\t{\n" +
                        "\t\t\t\"parent\": \"TestID\",\n" +
                        "\t\t\t\"hierarchy\": \"TestID > TESTIDPARENT\"\n" +
                        "\t\t}\n" +
                        "\t],\n" +
                        "\t\"contactName\": \"VISHAKA PALOUR\",\n" +
                        "\t\"latitude\": null,\n" +
                        "\t\"channel\": \"Retail\",\n" +
                        "\t\"outletType\": \"Convenience Outlet\",\n" +
                        "\t\"userName\": {\n" +
                        "\t\t\"activeStatus\": \"active\",\n" +
                        "\t\t\"activeStatusReason\": \"active\",\n" +
                        "\t\t\"designation\": [\n" +
                        "\t\t\t\"retailer\"\n" +
                        "\t\t],\n" +
                        "\t\t\"contactType\": \"retailer\",\n" +
                        "\t\t\"loginid\": \"TestID\",\n" +
                        "\t\t\"useraccountid\": \"TestID\",\n" +
                        "\t\t\"extendedAttributes\": {\n" +
                        "\t\t\t\"loyaltyFlag\": \"non loyalty\"\n" +
                        "\t\t},\n" +
                        "\t\t\"dob\": null,\n" +
                        "\t\t\"name\": \"VISHAKA PALOUR\",\n" +
                        "\t\t\"doa\": null,\n" +
                        "\t\t\"immediateParent\": [\n" +
                        "\t\t\t{\n" +
                        "\t\t\t\t\"parent\": \"TESTIDPARENT\"\n" +
                        "\t\t\t}\n" +
                        "\t\t]\n" +
                        "\t},\n" +
                        "\t\"extendedAttributes\": {\n" +
                        "\t\t\"PCPTier\": null,\n" +
                        "\t\t\"custOrder\": \"Y\",\n" +
                        "\t\t\"foodsTier\": null,\n" +
                        "\t\t\"PCPSubType\": null,\n" +
                        "\t\t\"custLoyalty\": \"N\",\n" +
                        "\t\t\"giftVoucher\": \"Y\",\n" +
                        "\t\t\"ITCProducts\": \"Y\",\n" +
                        "\t\t\"autoRedemption\": \"Y\",\n" +
                        "\t\t\"FCFoodsSubType\": null,\n" +
                        "\t\t\"supplierMapping\": [\n" +
                        "\t\t\t{\n" +
                        "\t\t\t\t\"UID\": \"C20220005809717\",\n" +
                        "\t\t\t\t\"RCSId\": \"181204899725\",\n" +
                        "\t\t\t\t\"CustID\": \"UK029\",\n" +
                        "\t\t\t\t\"SIFYID\": \"VI3493CIS722UK029\",\n" +
                        "\t\t\t\t\"WDDest\": \"VI3493\",\n" +
                        "\t\t\t\t\"WDName\": \"SRI DEVAKI LOGISTICS\",\n" +
                        "\t\t\t\t\"CatMapping\": \"\"\n" +
                        "\t\t\t}\n" +
                        "\t\t]\n" +
                        "\t},\n" +
                        "\t\"outletClass\": \"Retail Others\",\n" +
                        "\t\"outletName\": \"VISHAKA PALOUR\",\n" +
                        "\t\"activeStatus\": \"active\",\n" +
                        "\t\"outletCategory\": \"non loyalty\",\n" +
                        "\t\"displayAddress\": \"KARANAM GARI JN\",\n" +
                        "\t\"activeStatusReason\": \"active\",\n" +
                        "\t\"outletcode\": \"TestID\",\n" +
                        "\t\"longitude\": null\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }");
    }


}