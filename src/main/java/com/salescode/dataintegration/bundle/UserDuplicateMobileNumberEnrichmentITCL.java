package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.User;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;

public class UserDuplicateMobileNumberEnrichmentITCL extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User cdm) {
		if (NullUtils.isNotNull(cdm) && cdm.getDesignation().contains("retailer") && (cdm.getMobile() == null || cdm.getMobile().isEmpty())) {
			cdm.setVerified(false);
		}
		return new EnrichmentResult(EnrichmentResult.Status.OK, "Data enriched successfully");
	}
}
