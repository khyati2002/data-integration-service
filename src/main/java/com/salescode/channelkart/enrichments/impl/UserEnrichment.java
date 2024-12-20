/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.enrichments.impl;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.salescode.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.salescode.channelkart.models.*;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.*;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The class UserEnrichment.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class UserEnrichment extends AbstractEnrichment<User> {

	/** The userservice. */
	private final UserService userservice= SpringContext.getBean(UserService.class);

	/** The sso service. */
//	private final SSOUtils ssoService= SpringContext.getBean(SSOUtils.class);
	
	/**
	 * Apply.
	 *
	 * @param cdm the cdm
	 * @return the enrichment result
	 */
	@Override
	public EnrichmentResult apply(User cdm) {

		if(NullUtils.isNotNull(cdm)) {

			setPassword(cdm);
			
			String lob= setLob(cdm);
			
			CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccountInfo.class);
			CustomerAccountInfo customerAccount = customerService.getCustomerAccountInfo(lob);
			
			setLocationHierarchy(cdm, customerAccount);
			
			setSupplierMetadata(cdm);
			
			setActiveStatus(cdm);
			
			setActiveStatusreason(cdm);

			setSsoId(cdm);
			
			if(ObjectUtils.isEmpty(cdm.getImmediateParent())) {
				User admin= customerAccount.getAdmin();
				if(!cdm.getLoginId().equalsIgnoreCase(admin.getLoginId())) {
					HierarchyMetaDataService hierarchyMetaDataService = (HierarchyMetaDataService) ServiceLocator
							.lookup(HierarchyMetaData.class);
					Collection<HierarchyMetaData> adminMetaData = hierarchyMetaDataService
							.findByImmediateParent(admin.getLoginId());
					cdm.setImmediateParent(adminMetaData.stream().collect(Collectors.toList()));
					String hierarchy = customerService.getAdminHierarchy(cdm.getLoginId());
					cdm.setHierarchy(hierarchy);
					cdm.setNormalizedHierarchy(UserService.getNormalizedHierarchy(cdm.getHierarchy()));
				}
			}
			
			if(cdm.getLocationHierarchy() == null) {
				User admin= customerAccount.getAdmin();
				cdm.setLocationHierarchy(admin.getLocationHierarchy());
			}
			
			setDivisionRoles(cdm);
			
			return new EnrichmentResult(EnrichmentResult.Status.OK,"Data enriched successfully");
		}
		return new EnrichmentResult(EnrichmentResult.Status.ERROR,"Enrichment error: User not found null");
	}
	
	private void setPassword(User cdm) {
		if(cdm.getPassword() == null) {
			cdm.setPassword(userservice.getDefaultEncryptedUserPassword());
			if(cdm.getExtendedAttributes()!=null){
				ObjectNode extndAttribute= (ObjectNode) cdm.getExtendedAttributes();
				extndAttribute.remove(List.of("salt","encryptionAlgorithm"));
				cdm.setExtendedAttributes(extndAttribute);
			}
		}
	}
	
	private String setLob(User cdm) {
		String lob = cdm.getLob();
		if(lob==null){
			lob = SecurityContextUtils.getLob();
		}
		return lob;
	}
	
	private CustomerAccountInfo setLocationHierarchy(User cdm, CustomerAccountInfo customerAccount) {
		if(cdm.getLocationHierarchy() == null) {
			cdm.setLocationHierarchy(customerAccount.getAdmin().getLocationHierarchy());
		}
		
		return customerAccount;
	}
	
	private void setSupplierMetadata(User cdm) {
		if(cdm.getSupplierMetaData() != null && !cdm.getSupplierMetaData().isEmpty()) {
			SupplierMetaData supplierMetaData=cdm.getSupplierMetaData().get(0);
			int min = supplierMetaData.getMin() == null ? 0 : supplierMetaData.getMin().intValue();
			int max = supplierMetaData.getMax() == null ? 0 : supplierMetaData.getMax().intValue();
			if(min<=0 && max<=0 && supplierMetaData.getType()==null)
				cdm.setSupplierMetaData(new ArrayList<>());
		}
	}
	
	private void setActiveStatus(User cdm) {
		if(cdm.getActiveStatus() == null && StringUtils.isBlank(cdm.getId())) {
			cdm.setActiveStatus(ActiveStatus.ACTIVE);
		}
	}
	
	private void setActiveStatusreason(User cdm) {
		if(cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
			if(StringUtils.isBlank(cdm.getActiveStatusReason()) || 
					!cdm.getActiveStatusReason().startsWith("Deactivated")) {
				cdm.setActiveStatusReason("Deactivated by "+SecurityContextUtils.getPrincipal()+
						" on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
			}
		}else{
			if(StringUtils.isBlank(cdm.getActiveStatusReason()) || 
					cdm.getActiveStatusReason().startsWith("Deactivated")) {
				cdm.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
			}
		}
	}
	
	private void setSsoId(User cdm) {
		if(cdm.getSsoId() == null || cdm.getSsoId().equals("none")) {

			// needs to be added later
		}
	}
	
	/**
	 * Sets the roles if found empty from division permission group.
	 *
	 * @param user the new division roles
	 */
	private void setDivisionRoles(User user) {

		if(CollectionUtils.isEmpty(user.getRoles()) && CollectionUtils.isNotEmpty(user.getDesignation())) {
			DivisionService divisionService= SpringContext.getBean(DivisionService.class);
			List<Role> resultRoles= new ArrayList<>();
			user.getDesignation().forEach(divisonName->{
				List<Division> divisions= divisionService.findByDivisionName(divisonName);
				if(CollectionUtils.isNotEmpty(divisions)) {
					divisions.forEach(division->{
						List<Role> permissionRoles= division.getPermissionGroups();
						permissionRoles.forEach(role->{
							if(!resultRoles.contains(role)) {
								resultRoles.add(role);
							}
						});
					});
				}
			});
			if(CollectionUtils.isNotEmpty(resultRoles)) {
				user.setRoles(resultRoles);
			}
		}

	}

}
