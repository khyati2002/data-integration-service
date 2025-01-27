package com.applicate.unnati.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.json.JSONException;

public class OutletEnrichmentITCL extends AbstractEnrichment<OutletDetails> {

	@SuppressWarnings("all")
    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        if(NullUtils.isNotNull(cdm.getExtendedAttributes())) {
            JsonNode extAttr=cdm.getExtendedAttributes();
            ObjectNode newExtAttr=JSONUtils.getObjectMapper().createObjectNode();
            if((!extAttr.has("custOrder")) || (extAttr.has("custOrder") && "NULL".equalsIgnoreCase(extAttr.get("custOrder").asText()))) {
                newExtAttr.put("custOrder", "Y");
            }

//            if((!extAttr.has("autoRedemption")) || (extAttr.has("autoRedemption") && "NULL".equalsIgnoreCase(extAttr.get("autoRedemption").asText()))) {
//                newExtAttr.put("autoRedemption", "Y");
//            }

            if((!extAttr.has("custLoyalty")) || (extAttr.has("custLoyalty") && "NULL".equalsIgnoreCase(extAttr.get("custLoyalty").asText()))) {
                if (cdm.getOutletCategory().equals("loyalty")) {
                    newExtAttr.put("custLoyalty", "Y");
                }
            }

            if(cdm.getOutletCategory().equals("non loyalty"))newExtAttr.put("custLoyalty", "N");

            try {
                extAttr=JSONUtils.mergeJsonNodes( newExtAttr,extAttr);
            } catch (JSONException | IOException e) {
                return new EnrichmentResult(Status.ERROR, e.getMessage());
            }
            cdm.setExtendedAttributes(extAttr);
        }else {
            ObjectNode extAttr=JSONUtils.getObjectMapper().createObjectNode();
            extAttr.put("custOrder", "Y");
            //extAttr.put("autoRedemption", "Y");
            if (cdm.getOutletCategory().equals("loyalty")) {
                extAttr.put("custLoyalty", "Y");
            } else if (cdm.getOutletCategory().equals("non loyalty")) {
                extAttr.put("custLoyalty", "N");
            }
            cdm.setExtendedAttributes(extAttr);
        }
        return new EnrichmentResult(Status.OK, "Data enriched successfully");
    }

}