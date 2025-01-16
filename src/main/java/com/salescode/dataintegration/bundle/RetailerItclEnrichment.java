package com.salescode.dataintegration.bundle;


import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;

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
