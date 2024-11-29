package com.salescode.dataintegration.etl.impl;


import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;


public class OutletDetailsNameEnrichmentITCL extends AbstractEnrichment<CkOutletDetails> {

    @Override
    public EnrichmentResult apply(CkOutletDetails cdm) {
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
