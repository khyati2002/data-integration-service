package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;

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
        return new EnrichmentResult(EnrichmentResult.Status.OK,"Data enriched successfully");
    }

}
