package com.applicate.unnati.enrichment;



import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;

public class UserEnrichmentItcl extends AbstractEnrichment<User> {

    @Override
    public OperationResult.StepResult apply(User cdm) {
        if(cdm.getDesignation().contains("supplier")) {
            List<SupplierMetadata> supplierMetaDataList=cdm.getSupplierMetaData();
            if(supplierMetaDataList != null) {
                SupplierMetadata metaData=supplierMetaDataList.get(0);
                if(metaData.getExtendedAttributes() != null) {
                    JsonNode extAttr=metaData.getExtendedAttributes();
                    ObjectNode newExtAttr= JSONUtils.getObjectMapper().createObjectNode();
                    if((!extAttr.has("orderFunction")) || (extAttr.has("orderFunction") && "NULL".equalsIgnoreCase(extAttr.get("orderFunction").asText()))) {
                        newExtAttr.put("orderFunction", "Y");
                    }

                    if((!extAttr.has("orderFulfillmentTime")) || (extAttr.has("orderFulfillmentTime") && "NULL".equalsIgnoreCase(extAttr.get("orderFulfillmentTime").asText()))) {
                        newExtAttr.put("orderFulfillmentTime", "2");
                    }


                    try {
                        extAttr=JSONUtils.mergeJsonNodes(newExtAttr,extAttr);
                    } catch (IOException e) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
                    }
                    metaData.setExtendedAttributes(extAttr);
                }else {
                    ObjectNode extAttr=JSONUtils.getObjectMapper().createObjectNode();
                    extAttr.put("orderFunction", "Y");
                    extAttr.put("orderFulfillmentTime", "2");
                    metaData.setExtendedAttributes(extAttr);
                }
                if(metaData.getMin()==null) {
                    metaData.setMin(0);
                }
            }
            return OperationResult.StepResult.OK;
        }else{
            return OperationResult.StepResult.OK;
        }
    }
}