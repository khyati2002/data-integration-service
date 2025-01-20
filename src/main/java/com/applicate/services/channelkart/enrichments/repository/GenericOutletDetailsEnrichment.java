/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.services.channelkart.enrichments.repository;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.applicate.services.channelkart.component.model.SequenceGenerator;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.SequenceInfoService;
import com.applicate.services.channelkart.services.SpringContext;


import java.util.Date;

/**
 * The class GenericOutletDetailsEnrichment.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
public class GenericOutletDetailsEnrichment extends AbstractEnrichment<OutletDetails> {

	private SequenceInfoService sequenceService= SpringContext.getBean(SequenceInfoService.class);
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the enrichment result
	 */
	@Override
	public EnrichmentResult apply(OutletDetails cdm) {

		if(cdm.isActive()) {
			cdm.setActiveStatus(ActiveStatus.ACTIVE);
			if(StringUtils.isBlank(cdm.getActiveStatusReason()) ||
					cdm.getActiveStatusReason().startsWith("Deactivated")) {
				cdm.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
			}
		}else {
			cdm.setActiveStatus(ActiveStatus.INACTIVE);
			setActiveStatusReasonForInactiveOutlet(cdm);
		}

		if(cdm.getMapped() == null)
			cdm.setMapped(Boolean.FALSE);

		/*Sequence Generator*/
		if(sequenceService.shouldEnableSequenceGenerator(cdm.getClass().getSimpleName(), "outletCode",cdm.getOutletCode())) {
			cdm.setOutletCode(SequenceGenerator.Value.STRING.getDefaultValue());
		}

		return new EnrichmentResult(Status.OK,"Data enriched successfully");

	}

	private void setActiveStatusReasonForInactiveOutlet(OutletDetails cdm) {
		if(!StringUtils.isBlank(cdm.getActiveStatusReason()) && cdm.getActiveStatusReason().startsWith("REJECTED")) {
			cdm.setActiveStatusReason("REJECTED");
		}else {
			cdm.setActiveStatusReason("Deactivated by "+ SecurityContextUtils.getPrincipal()+
					" on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
		}

	}

}
