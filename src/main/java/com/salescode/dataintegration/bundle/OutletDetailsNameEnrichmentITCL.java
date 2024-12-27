package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.enrichments.Status;
import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.models.enums.ActiveStatus;

public class OutletDetailsNameEnrichmentITCL extends AbstractEnrichment<OutletDetails> {

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
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
