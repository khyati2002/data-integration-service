/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.validations.impl;


import com.salescode.channelkart.models.Division;
import com.salescode.channelkart.models.Role;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.services.DivisionService;
import com.salescode.channelkart.services.RoleService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.validations.ValidationResponseMessage;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserValidator extends AbstractValidationRule<User> {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private RoleService roleService = (RoleService) ServiceLocator.lookup(Role.class);
	/** The division service. */
	private DivisionService divisionService= (DivisionService) ServiceLocator.lookup(Division.class);
	
	
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the rule result
	 */
	@Override
	public RuleResult apply(User cdm) {
		List<String> errors= new ArrayList<>();
		Set<String> designations= cdm.getDesignation();
		if(ObjectUtils.isEmpty(designations)) {
			return new RuleResult(ValidationResult.Status.ERROR,com.salescode.channelkart.utils.StringUtils.format("For User : {}, Reason: Designation must not be empty",cdm.getLoginId()));
		}

		Set<String> unsupported= designations.stream().filter(element-> CollectionUtils.isEmpty(divisionService.findByDivisionName(element)))
						.collect(Collectors.toSet());
		if(ObjectUtils.isNotEmpty(unsupported)) {
			errors.add(com.salescode.channelkart.utils.StringUtils.format("User : {} (Cannot store user), Reason : [ Designation [{}] not found. Kindly re-verify input data.",
					cdm.getLoginId(), StringUtils.join(unsupported, ",")));
		}

		if(ObjectUtils.isNotEmpty(cdm.getRoles())){
			for ( Role objectRole : cdm.getRoles()) {
				if( roleService.getRole(objectRole.getName()).isEmpty() ){
					errors.add(com.salescode.channelkart.utils.StringUtils.format(ValidationResponseMessage.INVALID_USER_ROLE,objectRole.getName()));
				}
			}
		}

		if(cdm.getSsoId() != null && !cdm.getSsoId().equals("none")) {
		//	Optional<SSOConfiguration> ssoconfig= SSOUtils.get().getConfigurationByRegistrationId(cdm.getSsoId());
//			if(!ssoconfig.isPresent()) {
//				errors.add("Given sso id : "+cdm.getSsoId()+" not found. Please re-verify and retry with valid sso id");
//			}
		}
		
		if(!errors.isEmpty()) {
			String errorstr= com.salescode.channelkart.utils.StringUtils.format("Some values for User: {} voilating validations. Reason : {}", cdm.getLoginId(), org.apache.commons.lang.StringUtils.join(errors, ","));
			logger.error(errorstr);
			return new RuleResult(ValidationResult.Status.ERROR,errorstr);
		}
		return  RuleResult.OK;

	}

}
