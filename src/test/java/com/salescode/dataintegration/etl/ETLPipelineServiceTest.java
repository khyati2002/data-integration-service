package com.salescode.dataintegration.etl;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.DataIntegrationApplication;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
import com.salescode.dataintegration.etl.impl.TestEnrichment;
import com.salescode.dataintegration.etl.impl.TestTransformer;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.dataintegration.etl.transformer.registry.TransformerInfoRegistry;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = DataIntegrationApplication.class)
class ETLPipelineServiceTest {

    @Autowired DSLContext dslContext;
    @Autowired ETLPipelineService etlPipelineService;
    @Autowired @MockitoSpyBean MetadataRegistry metadataRegistry;
    @Autowired @MockitoSpyBean TransformerInfoRegistry transformerInfoRegistry;
    @Autowired @MockitoSpyBean EnrichmentInfoRegistry enrichmentInfoRegistry;
    @Autowired @MockitoSpyBean ETLRegistry etlRegistry;
    @Autowired EntityUtils entityUtils;

    @BeforeEach
    @SneakyThrows
    void setup(){
        CkMetadata ckMetadata = new CkMetadata();
        ckMetadata.setDomainValues(JSONUtils.getObjectMapper().readTree("[{\"dynamicKeys\":[\"outletcode\"]}]"));
        doReturn(Optional.of(ckMetadata)).when(metadataRegistry).getMetadataByDomainNameAndType("CkOutletDetails", "DynamicUniqueKey");
        CkTransformerInfo ckTransformerInfo = new CkTransformerInfo();
        ckTransformerInfo.setType("CkOutletDetails");
        ckTransformerInfo.setImplementation("com.salescode.dataintegration.etl.impl.TestTransformer");
        ckTransformerInfo.setActiveStatus(ActiveStatus.ACTIVE);
        doReturn(ckTransformerInfo).when(transformerInfoRegistry).getTransformerInfoById("testId");
        CkEnrichmentInfo ckEnrichmentInfo = new CkEnrichmentInfo();
        ckEnrichmentInfo.setType("CkOutletDetails");
        ckEnrichmentInfo.setImplementation("com.salescode.dataintegration.etl.impl.TestEnrichment");
        ckEnrichmentInfo.setActiveStatus(ActiveStatus.ACTIVE);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.PRE_VALIDATION);
        doReturn(List.of(ckEnrichmentInfo)).when(enrichmentInfoRegistry).getEnrichmentInfoByPhase(EnrichmentPhase.POST_VALIDATION);
        doReturn(new TestTransformer()).when(etlRegistry).getTransformer("com.salescode.dataintegration.etl.impl.TestTransformer");
        doReturn(new TestEnrichment()).when(etlRegistry).getEnrichment("com.salescode.dataintegration.etl.impl.TestEnrichment");
    }

    @Test
    void execute() {
        etlPipelineService.execute("""
                {
                    "groupId": "USR000008",
                    "lob": "mondelezckinduat",
                    "transformerInfo": [
                        {
                            "entityName": "CkOutletDetails",
                            "operationType": "insert",
                            "transformerId": "testId"
                        }
                    ],
                    "preserveOnFailure": true,
                    "features": [
                        {
                            "outletcode": 1076573
                        }
                    ]
                }""");
    }


}