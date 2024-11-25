package com.salescode.dataintegration.etl;

import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
import com.salescode.dataintegration.etl.impl.TestEnrichment;
import com.salescode.dataintegration.etl.impl.TestTransformer;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.impl.JoltTransformer;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import com.salescode.dis.FlinkApplication;
import com.salescode.dis.config.DatabaseConfig;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.JSON;
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
class ETLPipelineServiceJoltTest {

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
        CkMetadata ckMetadata = new CkMetadata();
        ckMetadata.setDomainValues(JSONUtils.getObjectMapper().readTree("[{\"dynamicKeys\":[\"outletcode\"]}]"));
        doReturn(Optional.of(ckMetadata)).when(metadataRegistry).getMetadataByDomainNameAndType("CkOutletDetails", "DynamicUniqueKey");
        CkTransformerInfo itcJoltTransformer = new CkTransformerInfo();
        itcJoltTransformer.setType("CkOutletDetails");
        itcJoltTransformer.setImplementation("com.salescode.dataintegration.etl.transformer.impl.JoltTransformer");
        itcJoltTransformer.setActiveStatus(ActiveStatus.ACTIVE);
        JSON joltCode = JSON.json("[\n" +
                "  {\n" +
                "    \"spec\": {\n" +
                "      \"supplierMapping\": {\n" +
                "        \"*\": {\n" +
                "          \"hierarchy\": \"=concat(@(1,UID),' > ',@(1,WDDest))\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operation\": \"modify-default-beta\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"spec\": {\n" +
                "      \"SIFYID\": \"=trim\"\n" +
                "    },\n" +
                "    \"operation\": \"modify-overwrite-beta\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"spec\": {\n" +
                "      \"supplierMapping\": {\n" +
                "        \"*\": {\n" +
                "          \"WDName\": \"\",\n" +
                "          \"CatMapping\": \"\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operation\": \"modify-default-beta\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"spec\": {\n" +
                "      \"UID\": [\n" +
                "        \"outletCode\",\n" +
                "        \"userName.loginId\",\n" +
                "        \"userName.userAccountId\"\n" +
                "      ],\n" +
                "      \"TYPE\": [\n" +
                "        \"outletCategory\",\n" +
                "        \"userName.extendedAttributes.loyaltyFlag\"\n" +
                "      ],\n" +
                "      \"#India\": [\n" +
                "        \"userName.locationHierarchy.country\",\n" +
                "        \"location.country\"\n" +
                "      ],\n" +
                "      \"Branch\": [\n" +
                "        \"userName.locationHierarchy.branch\",\n" +
                "        \"location.branch\"\n" +
                "      ],\n" +
                "      \"WDDest\": \"userName.immediateParent.[&1].immediateParent\",\n" +
                "      \"#active\": [\n" +
                "        \"userName.activeStatus\",\n" +
                "        \"userName.activeStatusReason\",\n" +
                "        \"activeStatus\",\n" +
                "        \"activeStatusReason\"\n" +
                "      ],\n" +
                "      \"PCPTier\": \"extendedAttributes.PCPTier\",\n" +
                "      \"CUSTName\": \"outletName\",\n" +
                "      \"DISTRICT\": [\n" +
                "        \"userName.locationHierarchy.district\",\n" +
                "        \"location.district\"\n" +
                "      ],\n" +
                "      \"#retailer\": [\n" +
                "        \"userName.designation.[]\",\n" +
                "        \"userName.contactType\"\n" +
                "      ],\n" +
                "      \"BirthDate\": \"userName.dob\",\n" +
                "      \"CustOrder\": \"extendedAttributes.custOrder\",\n" +
                "      \"FoodsTier\": \"extendedAttributes.foodsTier\",\n" +
                "      \"OutletLat\": \"latitude\",\n" +
                "      \"OwnerName\": [\n" +
                "        \"contactName\",\n" +
                "        \"userName.name\"\n" +
                "      ],\n" +
                "      \"OutletLong\": \"longitude\",\n" +
                "      \"OutletType\": \"outletType\",\n" +
                "      \"PCPSubType\": \"extendedAttributes.PCPSubType\",\n" +
                "      \"Anniversary\": [\n" +
                "        \"doo\",\n" +
                "        \"userName.doa\"\n" +
                "      ],\n" +
                "      \"ChannelType\": \"channel\",\n" +
                "      \"CustAddress\": [\n" +
                "        \"address\",\n" +
                "        \"displayAddress\"\n" +
                "      ],\n" +
                "      \"CustLoyalty\": \"extendedAttributes.custLoyalty\",\n" +
                "      \"GiftVoucher\": \"extendedAttributes.giftVoucher\",\n" +
                "      \"ITCProducts\": \"extendedAttributes.ITCProducts\",\n" +
                "      \"LoyaltyType\": \"outletClass\",\n" +
                "      \"AutoRedemption\": \"extendedAttributes.autoRedemption\",\n" +
                "      \"FCFoodsSubType\": \"extendedAttributes.FCFoodsSubType\",\n" +
                "      \"supplierMapping\": {\n" +
                "        \"*\": {\n" +
                "          \"UID\": [\n" +
                "            \"extendedAttributes.supplierMapping.[&1].UID\",\n" +
                "            \"immediateParent.[&1].immediateParent\"\n" +
                "          ],\n" +
                "          \"RCSID\": \"extendedAttributes.supplierMapping.[&1].RCSId\",\n" +
                "          \"CustID\": \"extendedAttributes.supplierMapping.[&1].CustID\",\n" +
                "          \"SIFYID\": \"extendedAttributes.supplierMapping.[&1].SIFYID\",\n" +
                "          \"WDDest\": [\n" +
                "            \"extendedAttributes.supplierMapping.[&1].WDDest\",\n" +
                "            \"userName.immediateParent.[&1].immediateParent\"\n" +
                "          ],\n" +
                "          \"WDName\": \"extendedAttributes.supplierMapping.[&1].WDName\",\n" +
                "          \"hierarchy\": \"immediateParent.[&1].hierarchy\",\n" +
                "          \"CatMapping\": \"extendedAttributes.supplierMapping.[&1].CatMapping\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"operation\": \"shift\"\n" +
                "  }\n" +
                "]") ;
        itcJoltTransformer.setCode(joltCode);
        doReturn(itcJoltTransformer).when(transformerInfoRegistry).getTransformerInfoById("unnati_csp_outlet_master_mdm");
        CkEnrichmentInfo ckEnrichmentInfo = new CkEnrichmentInfo();
        ckEnrichmentInfo.setType("CkOutletDetails");
        ckEnrichmentInfo.setImplementation("com.salescode.dataintegration.etl.impl.TestEnrichment");
        ckEnrichmentInfo.setActiveStatus(ActiveStatus.ACTIVE);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.PRE_VALIDATION);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.POST_VALIDATION);
        doReturn(new TestTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.impl.TestTransformer");
        doReturn(new JoltTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.transformer.impl.JoltTransformer");
        doReturn(new TestEnrichment()).when(etlRegistry).getEnrichment("com.salescode.dataintegration.etl.impl.TestEnrichment");
    }

    @Test
    void execute() {
        etlPipelineService.execute("{\n" +
                "  \"groupId\": \"2024-06-10\",\n" +
                "  \"lob\": \"unnati\",\n" +
                "  \"transformerInfo\": [\n" +
                "    {\n" +
                "      \"entityName\": \"CkOutletDetails\",\n" +
                "      \"transformerId\": \"unnati_csp_outlet_master_mdm\",\n" +
                "      \"operationType\": \"insert\",\n" +
                "      \"preprocessValidationExcludeGroup\": \"outlet_validation_exclude\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"topicName\": null,\n" +
                "  \"preserveOnFailure\": true,\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"UID\": \"C20220005809717\",\n" +
                "      \"CREATIONDATE\": \"2024-06-10 04:08:01.873\",\n" +
                "      \"PICKUPDATE\": null,\n" +
                "      \"DISTRICT\": \"EDIS\",\n" +
                "      \"Branch\": \"EVIZ\",\n" +
                "      \"CUSTName\": \"VISHAKA PALOUR\",\n" +
                "      \"OwnerName\": \"VISHAKA PALOUR\",\n" +
                "      \"ChannelType\": \"Retail\",\n" +
                "      \"OutletType\": \"Convenience Outlet\",\n" +
                "      \"LoyaltyType\": \"Retail Others\",\n" +
                "      \"FoodsTier\": null,\n" +
                "      \"PCPTier\": null,\n" +
                "      \"CustAddress\": \"KARANAM GARI JN\",\n" +
                "      \"CustState\": null,\n" +
                "      \"CustCity\": null,\n" +
                "      \"PIN\": null,\n" +
                "      \"Mobile\": null,\n" +
                "      \"BirthDate\": null,\n" +
                "      \"Anniversary\": null,\n" +
                "      \"PCPSubType\": null,\n" +
                "      \"FCFoodsSubType\": null,\n" +
                "      \"ITCProducts\": \"Y\",\n" +
                "      \"GiftVoucher\": \"Y\",\n" +
                "      \"OutletLat\": null,\n" +
                "      \"OutletLong\": null,\n" +
                "      \"CustOrder\": \"Y\",\n" +
                "      \"CustLoyalty\": \"N\",\n" +
                "      \"AutoRedemption\": \"Y\",\n" +
                "      \"Active\": \"Y\",\n" +
                "      \"TYPE\": \"non loyalty\",\n" +
                "      \"OutletName\": \"VISHAKA PALOUR\",\n" +
                "      \"supplierMapping\": [\n" +
                "        {\n" +
                "          \"CustID\": \"UK029\",\n" +
                "          \"SIFYID\": \"VI3493CIS722UK029\",\n" +
                "          \"WDDest\": \"VI3493\",\n" +
                "          \"UID\": \"C20220005809717\",\n" +
                "          \"RCSID\": \"181204899725\",\n" +
                "          \"WDName\": \"SRI DEVAKI LOGISTICS\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"loginId\": \"integration_user\",\n" +
                "  \"offset\": null,\n" +
                "  \"retryCount\": null,\n" +
                "  \"ignoreS3Log\": false,\n" +
                "  \"headersMap\": null\n" +
                "}");
    }


}