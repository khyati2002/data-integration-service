package com.salescode.dataintegration.etl;

import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.dataintegration.DataIntegrationApplication;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = DataIntegrationApplication.class)
class ETLPipelineServiceTest {

    @Autowired DSLContext dslContext;
    @Autowired ETLPipelineService etlPipelineService;
    @Autowired MetadataRegistry metadataRegistry;
    @Autowired EntityUtils entityUtils;


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
                            "transformerId": ""
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