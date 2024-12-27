package com.salescode.channelkart.enrichments.impl;

import com.salescode.channelkart.enrichments.AbstractEnrichment;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.models.CustomerAccountInfo;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.CustomerAccountsService;
import com.salescode.channelkart.services.ServiceLocator;
import org.apache.commons.lang3.StringUtils;

public class PhoneNumberEnrichment extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User user) {
		try {
		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator
				.lookup(CustomerAccountInfo.class);
		String lob = SecurityContextUtils.getLob();
		CustomerAccountInfo customerAccountInfo = customerService.getCustomerAccountInfo(lob);
		User admin = customerAccountInfo.getAdmin();

		if(StringUtils.isBlank(user.getCountryCode())) {
			user.setCountryCode(admin.getCountryCode());
		}

		if(StringUtils.isBlank(user.getDialCode())) {
			user.setDialCode(admin.getDialCode());
		}

		} catch (Exception e) {
			System.err.println("Error while setting dial code " + e.toString());
		}
		return EnrichmentResult.OK;
	}

	public static void main(String[] args) {
		User u = new User();
		u.setMobile("98817 63210");
		new PhoneNumberEnrichment().apply(u);
		System.out.println(u.getMobile());
	}

}
