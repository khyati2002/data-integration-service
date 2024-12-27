package com.salescode.dataintegration.bundle;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.enrichments.Status;
import com.salescode.channelkart.models.SupplierMetaData;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class UserEnrichmentItcl extends AbstractEnrichment<User> {

    @Override
    public EnrichmentResult apply(User cdm) {
        if(cdm.getDesignation().contains("supplier")) {
            List<SupplierMetaData> supplierMetaDataList=cdm.getSupplierMetaData();
            if(NullUtils.isNotNull(supplierMetaDataList)) {
                SupplierMetaData metaData=supplierMetaDataList.get(0);
                if(NullUtils.isNotNull(metaData.getExtendedAttributes())) {
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
                    } catch (JSONException | IOException e) {
                        return new EnrichmentResult(Status.ERROR, e.getMessage());
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
            return EnrichmentResult.OK;
        }else{
            return EnrichmentResult.OK;
        }
    }
}