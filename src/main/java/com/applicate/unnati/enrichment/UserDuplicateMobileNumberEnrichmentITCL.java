package com.applicate.unnati.enrichment;


import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.utils.NullUtils;

public class UserDuplicateMobileNumberEnrichmentITCL extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User cdm) {
		if (NullUtils.isNotNull(cdm) && cdm.getDesignation().contains("retailer") && (cdm.getMobile() == null || cdm.getMobile().isEmpty())) {
			cdm.setVerified(false);
		}
		return new EnrichmentResult(Status.OK, "Data enriched successfully");
	}
}
