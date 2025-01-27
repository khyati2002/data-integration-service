package com.applicate.unnati.enrichment;

import java.io.IOException;

import org.json.JSONException;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OutletDetailsNameEnrichmentITCL extends AbstractEnrichment<OutletDetails>{

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        // TODO Auto-generated method stub
        if(cdm.getContactName() == null) {
            cdm.setContactName(cdm.getOutletName());
        }
        if (cdm.getActiveStatus()==null)
        {
        	cdm.setActiveStatus(ActiveStatus.ACTIVE);
        	cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
        }
        return new EnrichmentResult(Status.OK,"Data enriched successfully");
    }

}
