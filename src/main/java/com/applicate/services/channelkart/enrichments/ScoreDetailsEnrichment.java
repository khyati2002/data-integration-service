package com.applicate.services.channelkart.enrichments;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.ScoreDetails;
import com.salescode.dim.jooq.impl.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

public class ScoreDetailsEnrichment extends AbstractEnrichment<ScoreDetails> {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final UserService userService = (UserService) ServiceLocator.lookup(User.class);
	private final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

	@Override
	public OperationResult.StepResult apply(ScoreDetails score) {
		log.info("<<< Enrichment for LoyaltyScore started >>>");
		try {
			if (score.getOutletCode() != null && score.getLoginId() == null) {
				OutletDetails outlet= outletDetailsService.findByOutletCode(score.getOutletCode());
				score.setLocationHierarchy(getLocationHierarchyIfExists(outlet));
				score.setLoginId((outlet.getUserName() == null)?null : outlet.getUserName().getLoginid());
			} else if ((score.getLoginId() != null && score.getOutletCode() != null)
					|| (score.getLoginId() != null && score.getOutletCode() == null)) {
				User user= userService.findByLoginId(score.getLoginId());
				score.setLocationHierarchy((user.getLocationHierarchy() == null)?null:user.getLocationHierarchy());
			} else {
			//	AuditLogger.log("Enrichment for LoyaltyScore", "<<<< LoginId and outletCode both can't be Null >>>>", AuditLogger.Status.FAILURE, "ScoreDetails", AuditLogger.Operations.UPDATE.toString(), null);
				return new OperationResult.StepResult(OperationResult.Status.OK, "LoginId and outletCode both can't be Null");
			}
			if(StringUtils.isNullOrBlank(score.getProgramNumber())) {
				ZoneId zoneId = ZoneId.systemDefault();

				long epochMillis =score.getStartDate().atZone(zoneId).toInstant().toEpochMilli();
				String programNumber = score.getFeature() + ":" + epochMillis + ":" + score.getLoginId();
				score.setProgramNumber(programNumber);
			}
			if (score.getFeature().toString().equalsIgnoreCase("loyalty")) {
				double totalEarnedPoints = score.getExtendedAttributes().get("totalEarnedPoints").asDouble();
				double openingPoints = score.getOpeningPoints();
				score.setTotalPoints(totalEarnedPoints + openingPoints);
				/*
				 * Truncate redeem data for cycle and user if score data requested again to
				 * upload
				 */
				log.info("<<<< redeemDataRecordValidator Start for Delete redeem data for cycle and user if score data requested again to upload >>>>");
			}
			/*
			 * Truncate Visibility score data for a cycle if Visibility score data re
			 * uploaded for the same cycle
			 *
			 */
			else if (score.getFeature().toString().equalsIgnoreCase("visibility")) {
				log.info("<<<< Start for Delete Visibility score data for a cycle if Visibility score data reuploaded for the same cycle >>>>");
				log.info("<<<< End for delete  Visibility score data for a cycle if Visibility score data reuploaded for the same cycle >>>>");
			}
		} catch (Exception e) {
			log.error("stacktrace", e);
		}
		return new  OperationResult.StepResult(OperationResult.Status.OK, "Score Status enriched");
	}

	private String getLocationHierarchyIfExists(OutletDetails outlet) {
		return outlet.getLocationHierarchy()==null?null:outlet.getLocationHierarchy();
	}
}
