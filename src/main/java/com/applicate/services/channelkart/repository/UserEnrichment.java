/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.repository;



import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.HierarchyMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.Division;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

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
	private final UserService userservice = (UserService) ServiceLocator.lookup(User.class);


	
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
			
			CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);
//			CustomerAccountInfo customerAccount = customerService.getCustomerAccountInfo(lob);
//
//			setLocationHierarchy(cdm, customerAccount);
			
			setSupplierMetadata(cdm);
			
			setActiveStatus(cdm);
			
			setActiveStatusreason(cdm);

			setSsoId(cdm);
			
			if(ObjectUtils.isEmpty(cdm.getImmediateParent())) {
				com.salescode.dim.jooq.generated.tables.pojos.User admin= customerService.getAdminInfo();
				if(!cdm.getLoginid().equalsIgnoreCase(admin.getLoginid())) {
					HierarchyMetadataService hierarchyMetaDataService = (HierarchyMetadataService) ServiceLocator
							.lookup(HierarchyMetadata.class);
					Collection<HierarchyMetadata> adminMetaData = hierarchyMetaDataService
							.findByImmediateParent(admin.getLoginid());
					cdm.setImmediateParent(adminMetaData.stream().collect(Collectors.toList()));
					String hierarchy = customerService.getAdminHierarchy(cdm.getLoginid());
					cdm.setHierarchy(hierarchy);
					cdm.setNormalizedHierarchy(UserService.getNormalizedHierarchy(cdm.getHierarchy()));
				}
			}
			
			if(cdm.getLocationHierarchy() == null) {
				com.salescode.dim.jooq.generated.tables.pojos.User admin= customerService.getAdminInfo();
				cdm.setLocationHierarchy(admin.getLocationHierarchy());
			}
			
			setDivisionRoles(cdm);
			
			return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
		}
		return new OperationResult.StepResult(OperationResult.Status.ERROR,"Enrichment error: User not found null");
	}
	
	private void setPassword(User cdm) {
		if(cdm.getPassword() == null) {
			cdm.setPassword(userservice.DEFAULT_ENCODED_PASSWORD);
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
		//	lob = SecurityContextUtils.getLob();
			lob = "ckunnatiuat";
		}
		return lob;
	}
	
	private CustomerAccount setLocationHierarchy(User cdm, CustomerAccount customerAccount) {
		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);
		if(cdm.getLocationHierarchy() == null) {
			cdm.setLocationHierarchy(customerService.getAdminInfo().getLocationHierarchy());
		}
		
		return customerAccount;
	}
	
	private void setSupplierMetadata(User cdm) {
		if(cdm.getSupplierMetaData() != null && !cdm.getSupplierMetaData().isEmpty()) {
			SupplierMetadata supplierMetaData=cdm.getSupplierMetaData().get(0);
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
				cdm.setActiveStatusReason("Deactivated by "); //+SecurityContextUtils.getPrincipal()+
		//				" on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
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
//			Optional<SSOConfiguration> ssoconfig= ssoService.getConfigurationByUserDesignation(cdm);
//			if(ssoconfig.isPresent()) {
//				cdm.setSsoId(ssoconfig.get().getRegistrationId());
//			}
		}
	}
	
	/**
	 * Sets the roles if found empty from division permission group.
	 *
	 * @param user the new division roles
	 */
	private void setDivisionRoles(User user) {

		if(CollectionUtils.isEmpty(user.getRoles()) && CollectionUtils.isNotEmpty(user.getDesignation())) {
//			DivisionService divisionService= SpringContext.getBean(DivisionService.class);
//			List<AuthRole> resultRoles= new ArrayList<>();
//			user.getDesignation().forEach(divisonName->{
//				List<Division> divisions= divisionService.findByDivisionName(divisonName);
//				if(CollectionUtils.isNotEmpty(divisions)) {
//					divisions.forEach(division->{
//						List<AuthRole> permissionRoles= division.getPermissionGroups();
//						permissionRoles.forEach(role->{
//							if(!resultRoles.contains(role)) {
//								resultRoles.add(role);
//							}
//						});
//					});
//				}
//			});
//			if(CollectionUtils.isNotEmpty(resultRoles)) {
//				user.setRoles(resultRoles);
//			}
		}

	}

}
