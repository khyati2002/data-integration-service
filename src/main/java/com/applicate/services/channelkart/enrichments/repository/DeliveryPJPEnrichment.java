package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.DeliveryPJP;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import java.util.Date;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliveryPJPEnrichment extends AbstractEnrichment<DeliveryPJP>{
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public EnrichmentResult apply(DeliveryPJP pjp) {
		logger.info("Enrichment for deliverypjp started : ");
		try {
			UserService userService = SpringContext.getBean(UserService.class);
			OutletDetailsService outletService = SpringContext.getBean(OutletDetailsService.class);
			DeliveryPJPService pjpService = SpringContext.getBean(DeliveryPJPService.class);
			
			if(pjp.getLoginId()!=null && !pjp.getLoginId().equals("")) {
				setDesignation(pjp,userService);
			}
			if(pjp.getOutletCode()!=null) {
				setOutletDetails(pjp,userService,outletService);
			}
			Date pjpDate = pjp.getPjpDate();
			if(pjpDate!=null) {
				pjpService.addDayAndFrequency(pjp);
			}
		}catch (Exception e) {
			return new EnrichmentResult(Status.ERROR, e.getMessage());
		}
		return new EnrichmentResult(Status.OK,"Delivery PJP Data enriched successfully");
	}
	
	private void setDesignation(DeliveryPJP pjp,UserService userService) {
		User resultFromDb=userService.findByLoginId(pjp.getLoginId());
		if(resultFromDb!=null) {
			Set<String> designations = resultFromDb.getDesignation();
			String designationName = null;
			if(designations!=null && !CollectionUtils.isEmpty(designations)) {
				designationName = designations.iterator().next();
			}
			pjp.setDesignation(designationName!=null?designationName.toLowerCase():null);
		}else {
			pjp.setLoginId(null);
		}
	}
	
	private void setOutletDetails(DeliveryPJP pjp,UserService userService,OutletDetailsService outletService) {

		OutletDetails outletFromDB = outletService.findByOutletCode(pjp.getOutletCode());
		if(outletFromDB!=null) {
			String outletLoginId = outletFromDB.getUserName()!=null?outletFromDB.getUserName().getLoginId():null;
			if(StringUtils.isValidString(outletLoginId)) {
				User resultFromDb=userService.findByLoginId(outletLoginId);
				if(resultFromDb!=null)
					outletFromDB.setUserName(resultFromDb);
			}else {
				outletFromDB.setUserName(null);
				logger.info("login id is null for outlet code {} ",pjp.getOutletCode());
			}
		}else {
			pjp.setOutletCode(null);
		}
	
	}

}
