package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.enrichments.Status;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.enums.ActiveStatus;

public class RetailerItclEnrichment extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User cdm) {
		Integer version = cdm.getVersion();
		if(version==null)
		{
			cdm.setActiveStatus(ActiveStatus.ACTIVE);
		    cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
		}
		return new EnrichmentResult(Status.OK," User Data enriched successfully");	}

}
