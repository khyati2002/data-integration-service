package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;

public class RetailerItclEnrichment extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User cdm) {
		Integer version = cdm.getVersion();
		if(version==null)
		{
			cdm.setActiveStatus(ActiveStatus.ACTIVE);
		    cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
		}
		return new EnrichmentResult(EnrichmentResult.Status.OK," User Data enriched successfully");	}

}
