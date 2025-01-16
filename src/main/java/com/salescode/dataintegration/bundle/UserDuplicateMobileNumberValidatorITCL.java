package com.salescode.dataintegration.bundle;



import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.applicate.services.channelkart.validations.AbstractRule;

import java.util.List;
import java.util.Optional;

public class UserDuplicateMobileNumberValidatorITCL extends AbstractRule<User> {

	String regex = "(^[0-9]{10}$)";

	@SuppressWarnings("all")
	@Override
	public RuleResult apply(User cdm) {
		UserService userService = SpringContext.getBean(UserService.class);
		StringBuilder ruleResult = new StringBuilder();
		User user = userService.findByLoginId(cdm.getLoginId());
		if(SecurityContextUtils.getPrincipal().equalsIgnoreCase("integration_user")) {
			return RuleResult.OK;
		}
		if (cdm.getMobile() != null) {
			if (user!= null && user.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
				ruleResult.append("User is inactive in the system");
				return new RuleResult(Status.ERROR, ruleResult.toString());
			}
		}
		if (cdm != null && cdm.getDesignation()!=null && cdm.getDesignation().contains("retailer")) {
			if (cdm.getMobile() != null) {
				if (!cdm.getMobile().isEmpty() && !checkMobileNumberPattern(cdm.getMobile())) {
					ruleResult.append("Mobile number field allowed only 10 digit valid number or blank.");
					return new RuleResult(Status.ERROR, ruleResult.toString());
				}
				if (!StringUtils.isNullOrBlank(cdm.getMobile()) && cdm.getDesignation().contains("retailer")) {
					Optional<List<User>> currentUsersObject = userService.findByMobileSafely(cdm.getMobile());
					if (currentUsersObject.isPresent()) {
						List<User> currentUsers = currentUsersObject.get();
						for (User currentUser : currentUsers) {
							if (!(currentUser.getLoginId().equals(cdm.getLoginId())) && currentUser.getDesignation().contains("retailer")) {
								return new RuleResult(Status.ERROR, "Entered mobile number is already registered with another user: " + currentUser.getLoginId()+". Please try with a different number.");
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
