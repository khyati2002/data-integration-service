package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.enrichments.Status;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.utils.NullUtils;

public class UserDuplicateMobileNumberEnrichmentITCL extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User cdm) {
		if (NullUtils.isNotNull(cdm) && cdm.getDesignation().contains("retailer") && (cdm.getMobile() == null || cdm.getMobile().isEmpty())) {
			cdm.setVerified(false);
		}
		return new EnrichmentResult(Status.OK, "Data enriched successfully");
	}
}
