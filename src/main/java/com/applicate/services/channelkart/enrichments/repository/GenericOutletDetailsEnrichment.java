/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.services.channelkart.enrichments.repository;



import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.StringUtils;

/**
 * The class GenericOutletDetailsEnrichment.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
public class GenericOutletDetailsEnrichment extends AbstractEnrichment<OutletDetails> {

	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the enrichment result
	 */
	@Override
	public OperationResult.StepResult apply(OutletDetails cdm) {

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

//		/*Sequence Generator*/
//		if(sequenceService.shouldEnableSequenceGenerator(cdm.getClass().getSimpleName(), "outletcode",cdm.getOutletcode())) {
//			cdm.setOutletcode(SequenceGenerator.Value.STRING.getDefaultValue());
//		}

		return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");

	}
	
	private void setActiveStatusReasonForInactiveOutlet(OutletDetails cdm) {
		if(!StringUtils.isBlank(cdm.getActiveStatusReason()) && cdm.getActiveStatusReason().startsWith("REJECTED")) {
			   cdm.setActiveStatusReason("REJECTED");
		   }else {
			   cdm.setActiveStatusReason("Deactivated by "); //+ SecurityContextUtils.getPrincipal()+
//						" on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
		   }
	
	}

}
