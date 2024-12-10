package com.salescode.channelkart.validations.impl;



import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.UserService;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.channelkart.validations.repository.RegexValidation;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.jooq.impl.CkOutletDetails;
import com.salescode.jooq.impl.CkUser;

import java.util.List;
import java.util.Optional;

public class DuplicateMobileNumberValidatorITCL extends AbstractValidationRule<CkOutletDetails> {

	String regex = "(^[0-9]{10}$)";

	@SuppressWarnings("all")
	@Override
	public RuleResult apply(CkOutletDetails cdm) {
		UserService userService = SpringContext.getBean(UserService.class);
		StringBuilder ruleResult = new StringBuilder();
		CkUser user = userService.findByLoginId(cdm.getOutletcode());
//		if(SecurityContextUtils.getPrincipal().equalsIgnoreCase("integration_user")) {
//			return RuleResult.OK;
//		}
		if (user != null && user.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
			ruleResult.append("User is inactive in the system");
			return new RuleResult(ValidationResult.Status.ERROR, ruleResult.toString());
		}
		if (user.getDesignation()!=null && user.getDesignation().contains("retailer")) {
			if (cdm.getUserName().getMobile() != null) {
				if (!cdm.getUserName().getMobile().isEmpty() && !checkMobileNumberPattern(cdm.getUserName().getMobile())) {
					ruleResult.append("Mobile number field allowed only 10 digit valid number or blank.");
					return new RuleResult(ValidationResult.Status.ERROR, ruleResult.toString());
				}
				if (!StringUtils.isNullOrBlank(cdm.getUserName().getMobile())) {
					Optional<List<CkUser>> currentUsersObject = userService.findByMobileSafely(cdm.getUserName().getMobile());
					if (currentUsersObject.isPresent() && !currentUsersObject.get().isEmpty()) {
						List<CkUser> currentUsers = currentUsersObject.get();
						for (CkUser currentUser : currentUsers) {
							if (!(currentUser.getLoginid().equals(user.getLoginid())) && currentUser.getDesignation().contains("retailer")) {
								return new RuleResult(ValidationResult.Status.ERROR, "Entered mobile number is already registered with another user: " + currentUser.getLoginid() + ". Please try with a different number.");
							}
						}
					}
				}
			}
		}
		return RuleResult.OK;
	}

	public boolean checkMobileNumberPattern(String mobile) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(regex, mobile);
	}
}
