package com.applicate.services.channelkart.enrichments.repository;


import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.User;
import org.apache.commons.lang3.StringUtils;

public class PhoneNumberEnrichment extends AbstractEnrichment<User> {

	@Override
	public EnrichmentResult apply(User user) {
		try {
		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator
				.lookup(CustomerAccount.class);

		User admin = customerService.getAdminInfo();
		
		if(StringUtils.isBlank(user.getCountryCode())) {
			user.setCountryCode(admin.getCountryCode());
		}
		
		if(StringUtils.isBlank(user.getDialCode())) {
			user.setDialCode(admin.getDialCode());
		}
		
		} catch (Exception e) {
			System.err.println("Error while setting dial code " + e.toString());
		}
		return OperationResult.StepResult.OK;
	}

	public static void main(String[] args) {
		User u = new User();
		u.setMobile("98817 63210");
		new PhoneNumberEnrichment().apply(u);
		System.out.println(u.getMobile());
	}

}
