package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.component.model.SequenceGenerator;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.SequenceInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.SequenceInfo;
import org.jooq.tools.StringUtils;

import java.util.Date;

public class GenericOutletDetailsEnrichment {

	private SequenceInfoService sequenceService= (SequenceInfoService) ServiceLocator.lookup(SequenceInfo.class);
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the enrichment result
	 */
	public EnrichmentResult apply(OutletDetails cdm) {

		if(cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)) {
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
		if(sequenceService.shouldEnableSequenceGenerator(cdm.getClass().getSimpleName(), "outletCode",cdm.getOutletcode())) {
			cdm.setOutletCode(SequenceGenerator.Value.STRING.getDefaultValue());
		}

		return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");

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
