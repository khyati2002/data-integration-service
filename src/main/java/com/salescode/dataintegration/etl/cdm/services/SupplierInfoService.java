/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.dataintegration.etl.cdm.services;

import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.dto.SupplierInfo;
import com.applicate.services.channelkart.dto.SuppliersMinimalDTO;
import com.applicate.services.channelkart.models.ChannelHierarchyMetaData;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The class SupplierInfoService.
 *
 * @author Manish Srivastava
 * @since  Aug 2020
 */
@Service
public class SupplierInfoService {
	
	private static Logger logger= LoggerFactory.getLogger(SupplierInfoService.class);

	/** The user service. */
	@Autowired
	private UserService userService;
	
	@Autowired
	private OutletDetailsService outletService;

	@Autowired
	private DeliveryPJPService pjpService;

	@Autowired
	private ChannelHierarchyMetaDataService channelHierarchyService;
	
	@Autowired
	private DistributedCache distributedCache;



	/** The cache domain. */
	private static final String CACHE_DOMAIN= "suppliers";


	/**
	 * Find suppliers.
	 *
	 * @param outletcode
	 * @return List of suppliers associated with the outletcode
	 */
	public List<String> findSuppliersUsingOutletCode(String outletcode) {
		return distributedCache.withCache(SecurityContextUtils.getLob(),CACHE_DOMAIN, "o:"+outletcode, outlet ->{
			OutletDetails outletdetails= outletService.findByOutletCode(outletcode);
			return findSuppliers(outletdetails);
		});
	}
	
	/**
	 * Find suppliers.
	 *
	 * @param username
	 * @return List of suppliers associated with the username
	 */
    public List<String> findSuppliersUsingUserName(String username) {
		  return distributedCache.withCache(SecurityContextUtils.getLob(),CACHE_DOMAIN, "u:"+username, user ->{
				User userdata= userService.findByLoginId(username);
				return findSuppliers(userdata);
			});
	}
	
	/**
	 * Find suppliers.
	 *
	 * @param outletCode
	 * @return List of suppliers associated with the outletCode
	 */
	public List<SupplierInfo> findSuppliersByOutlet(String outletCode) {
		OutletDetails outletdetails= outletService.findByOutletCode(outletCode);
		List<String> suppliers = findSuppliers(outletdetails);	
		if(!suppliers.isEmpty()) {
			return userService.findByLoginIdIn(suppliers).stream().map(SupplierInfo::new).collect(Collectors.toList());
		}else {
			return new ArrayList<>();
		}
	}
	
	/**
	 * Find suppliers.
	 *
	 * @param loginId
	 * @return List of suppliers associated with the loginid
	 */
	public List<SupplierInfo> findSuppliersByLoginId(String loginId)  {
		List<String> suppliers = findSuppliersUsingUserName(loginId);
		if(!suppliers.isEmpty()) {
			return userService.findByLoginIdIn(suppliers).stream().map(SupplierInfo::new).collect(Collectors.toList());
		}else {
			return new ArrayList<>();
		}
	}


	public void findNextDeliveyDate(String outletCode,List<SupplierInfo> supplierInfos){
		supplierInfos.forEach(s->{
			try {
				Long l = pjpService.findDeliveryDateForOrder(outletCode, s.getLoginId());
				if (l != null && l>0) {
					s.setNextPjpDate(new Date(l));
				}
			}catch (Exception e){
				logger.error("failed to fetch delivery pjp info for supplierid {} and outletcode {}",s.getLoginId(),e);
			}
		});
	}

	public void findNextDeliveyDateForMinimal(String outletcode, List<SuppliersMinimalDTO> supplierMinimalInfos){
		supplierMinimalInfos.forEach(supplierMinimalInfo->{
			try {
				Long longDate = pjpService.findDeliveryDateForOrder(outletcode, supplierMinimalInfo.getLoginId());
				if (longDate != null && longDate>0) {
					supplierMinimalInfo.setNextPjpDate(new Date(longDate));
				}
			}catch (Exception e){
				logger.error("failed to fetch delivery pjp info for supplierid {} and minimal outletcode {}",supplierMinimalInfo.getLoginId(),e);
			}
		});
	}

	public List<String> findSuppliers(OutletDetails outlet) {
		if(outlet != null) {
			Collection<ChannelHierarchyMetaData> parent= channelHierarchyService.getOutletChannelHierarchy(outlet);
			Set<String> suppliers= new HashSet<>();
			for (ChannelHierarchyMetaData data : parent){
				if(data.getLevel1Supplier() != null)
					suppliers.add(data.getLevel1Supplier());
				if(data.getLevel2Supplier() != null)
					suppliers.add(data.getLevel2Supplier());
				if(data.getLevel3Supplier() != null)
					suppliers.add(data.getLevel3Supplier());
			}
			return new ArrayList<>(suppliers);
		}else {
			logger.error("Cannot find outlet for fetching suppliers");
		}

		return List.of();
	}
	
	public List<String> findSuppliers(User user) {
		if(user != null) {
			Collection<ChannelHierarchyMetaData> parent= channelHierarchyService.getUserChannelHierarchy(user);
			Set<String> suppliers= new HashSet<>();
			for (ChannelHierarchyMetaData data : parent){
				if(data.getLevel1Supplier() != null)
					suppliers.add(data.getLevel1Supplier());
				if(data.getLevel2Supplier() != null)
					suppliers.add(data.getLevel2Supplier());
				if(data.getLevel3Supplier() != null)
					suppliers.add(data.getLevel3Supplier());
			}
			return new ArrayList<>(suppliers);
		}else {
			logger.error("Cannot find user for fetching supplier info");
		}

		return List.of();
	}
	
	/**
	 * Clear cache.
	 *
	 * @param lob the lob
	 * @param cacheKey the cache key
	 */
	public void clearCache(String lob, String cacheKey) {
		distributedCache.clearCache(lob, CACHE_DOMAIN, cacheKey);
	}
	
	/**
	 * Clear cache.
	 *
	 * @param lob the lob
	 */
	public void clearCache() {
		distributedCache.clearCache(SecurityContextUtils.getLob(), CACHE_DOMAIN);
	}

}
