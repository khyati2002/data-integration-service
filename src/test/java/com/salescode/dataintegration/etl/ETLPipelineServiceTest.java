package com.salescode.dataintegration.etl;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
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

@SpringBootTest(classes = FlinkApplication.class)
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
                "                        {\n" +
                "                            \"outletcode\": 1076573\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }");
    }


}