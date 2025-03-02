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
        CkMetadata ckMetadata = new CkMetadata();
       // ckMetadata.setDomainValues(JSONUtils.getObjectMapper().readTree("[{\"dynamicKeys\":[\"outletcode\"]}]"));
        //doReturn(Optional.of(ckMetadata)).when(metadataRegistry).getMetadataByDomainNameAndType("CkOutletDetails", "DynamicUniqueKey");
        CkTransformerInfo ckTransformerInfo = new CkTransformerInfo();
        ckTransformerInfo.setType("CkSchemeDefination");
        ckTransformerInfo.setImplementation("com.salescode.dataintegration.bundle.transformer.HCCBTransformer");
        ckTransformerInfo.setActiveStatus(ActiveStatus.ACTIVE);
        doReturn(ckTransformerInfo).when(transformerInfoRegistry).getTransformerInfoById("mdm_scheme_hccb");
        CkEnrichmentInfo ckEnrichmentInfo = new CkEnrichmentInfo();
        ckEnrichmentInfo.setType("CkSchemeDefination");
        ckEnrichmentInfo.setImplementation("com.salescode.dataintegration.bundle.TestEnrichment");
        ckEnrichmentInfo.setActiveStatus(ActiveStatus.ACTIVE);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.PRE_VALIDATION);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.POST_VALIDATION);
        doReturn(new TestTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.impl.TestTransformer");
        doReturn(new OutletDetailsNameEnrichmentITCL()).when(etlRegistry).getEnrichment("com.salescode.dataintegration.bundle.TestEnrichment");

    }

    @Test
    void execute() {
        etlPipelineService.execute("{\"requestId\":\"0b53c361-f8c7-4528-8ae9-025a7a232e15\",\"groupId\":\"prod/PromotionMaster_B030_20250203040348_I.zip\",\"fileId\":\"1780254bec300f1d6cdee9484f80807a\",\"lob\":\"kbuddy\",\"submittedBy\":\"integration_user\",\"transformerInfo\":[{\"entityName\":\"SchemeDefination\",\"transformerId\":\"mdm_scheme_hccb\",\"operationType\":\"insert\",\"preprocessValidationExcludeGroup\":\"\",\"skipPreprocessing\":\"false\"}],\"topicName\":null,\"preserveOnFailure\":true,\"features\":[{\"scheme_no\":\"77992741\",\"scheme_line_no\":\"1\",\"distributor_channel\":\"Z1\",\"dist_sap_customer_id\":\"0504447486\",\"mer_wef\":\"2024-02-24 00:00:00\",\"mer_wet\":\"2025-02-02 23:59:59\",\"disbursement_method\":\"1 \",\"disbursement_method_desc\":\"Spot\",\"calculation_method\":\"4 \",\"calculation_method_desc\":\"Free Bottle\",\"monitoring_scope\":\"3 \",\"monitoring_scope_desc\":\"NA\",\"monitoring_uom\":\"EA\",\"monitoring_slab_from\":\"30\",\"monitoring_slab_to\":\"30\",\"market_scope\":\"3 \",\"market_scope_desc\":\"0504447486\",\"discounted_value\":\"3\",\"discounted_item_id\":\"000000000000104593\",\"discounted_item_uom\":\"EA\",\"external_id\":\"77992741_1_HMA1_0504447486_G34\",\"isprogresiveslab,\":\"0 \",\"discountedfoctype\":\"0\",\"discountedfoctypedesc\":\"NA\",\"discountedprice\":\"0\",\"exclusion\":\"NA\",\"scheme_desc\":\"BUY 30 to 30 CS GET 3 Bottles free per CS\",\"monitoring_value\":\"77992741\"}],\"loginId\":\"applicate\",\"offset\":null,\"retryCount\":null,\"ignoreS3Log\":true,\"headersMap\":null}");
    }


}