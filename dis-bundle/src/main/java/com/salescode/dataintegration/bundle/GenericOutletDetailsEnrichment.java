/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.dataintegration.bundle;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.salescode.channelkart.component.model.SequenceGenerator;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.dataintegration.etl.cdm.services.SequenceInfoService;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;

import java.util.Date;

/**
 * The class GenericOutletDetailsEnrichment.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
public class GenericOutletDetailsEnrichment extends AbstractEnrichment<CkOutletDetails> {

	private SequenceInfoService sequenceService= SpringContext.getBean(SequenceInfoService.class);
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the enrichment result
	 */
	@Override
	public EnrichmentResult apply(CkOutletDetails cdm) {

		if(cdm.getActiveStatus().equals("active")) {
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
		if(sequenceService.shouldEnableSequenceGenerator(cdm.getClass().getSimpleName(), "outletcode",cdm.getOutletcode())) {
			cdm.setOutletcode(SequenceGenerator.Value.STRING.getDefaultValue());
		}

		return new EnrichmentResult(EnrichmentResult.Status.OK,"Data enriched successfully");

	}
	
	private void setActiveStatusReasonForInactiveOutlet(CkOutletDetails cdm) {
		if(!StringUtils.isBlank(cdm.getActiveStatusReason()) && cdm.getActiveStatusReason().startsWith("REJECTED")) {
			   cdm.setActiveStatusReason("REJECTED");
		   }else {
			   cdm.setActiveStatusReason("Deactivated by "); //+ SecurityContextUtils.getPrincipal()+
//						" on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
		   }
	
	}

}
