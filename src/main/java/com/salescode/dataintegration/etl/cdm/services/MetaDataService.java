/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.dataintegration.etl.cdm.services;

//import com.applicate.analytics.exception.AccessDeniedException;
//import com.applicate.services.channelkart.cache.AppCacheManager;
//import com.applicate.services.channelkart.cache.CacheOperationsConstant;
//import com.applicate.services.channelkart.cache.DistributedCache;
//import com.applicate.services.channelkart.cache.RequestCacheManager;
//import com.applicate.services.channelkart.client.properties.PropertyRegistry;
//import com.applicate.services.channelkart.exceptions.checked.NoSuchElementException;
//import com.applicate.services.channelkart.templates.service.TemplateService;
//
//import com.salescode.channelkart.services.SpringContext;
//import com.salescode.channelkart.utils.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.MetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MetaDataService extends AbstractCDMService<CkMetadata>
{
	private static final String CACHE_DOMAIN = "metadata";
	private static final Set<String> appConfigDomainNameSet = Set.of("clientconfig","filter", "supportedValues");

    //	private RequestCacheManager requestCache;
//
//	public static final String OTP_VERIFY = "otpverify";
//
//	@SuppressWarnings("unused")
//	private DistributedCache distributedCache;

	MetaDataRepository metaDataRepository;

	private static final Map<String,String> playgroudDomainName = Map.of("clientconfig", "playground_config");

	public MetaDataService(MetaDataRepository repository
//            , DistributedCache distributedCache
//            , RequestCacheManager requestCache
    )
    {
//		super(repository);
		this.metaDataRepository = repository;
//		this.distributedCache = distributedCache;
//		this.requestCache= requestCache;
	}

//	public void refresh() {
//		clear();
//		fetchAll();
//	}
//
//	public List<CkMetadata> fetchAll() {
//		return fetchAll(false);
//	}
//
//	public List<CkMetadata> fetchAll(boolean fetchRootData) {
//		String lob = SecurityContextUtils.getLob();
//		if(fetchRootData) {
//			//lob = "default";
//			lob= null;
//		}
//		return distributedCache.withCache(lob,CACHE_DOMAIN,(s)-> metaDataRepository.findAll());
//	}
//
//	public List<CkMetadata> fetchAll(String domainName,boolean fetchRootData) {
//		return fetchAll(fetchRootData).stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName)).collect(Collectors.toList());
//	}
//
//	public CkMetadata fetchByValue(String lob,String domainName,String domainType) {
//		return SecurityContextUtils.switchWithLOB(lob, () -> fetchByValue(domainName, domainType));
//	}

	public CkMetadata fetchByValueFromDB(String domainName,String domainType) {
		return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
	}

//	public CkMetadata fetchByValue(String domainName,String domainType) {
//		return fetchByValue(domainName, domainType, false);
//	}
//
	public CkMetadata fetchByValue(String domainName,String domainType,boolean cached) {
		//return AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob()+":"+domainName,domainType,(s)->fetchByValueFromDB(domainName,domainType));
        return fetchByValueFromDB(domainName,domainType);
		//return fetchAll().stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName) && f.getDomainType().equalsIgnoreCase(domainType)).findFirst().orElse(null);
	}
//
//	public void onCacheChange(CkMetadata metaData) {
//		distributedCache.clearCache(SecurityContextUtils.getLob(), null, CACHE_DOMAIN);
//		SpringContext.getBeanSafely(TemplateService.class).ifPresent(templateService -> templateService.onMetaDataChange(metaData));
//		SpringContext.getBean(PropertyRegistry.class).clear();
//	}
//
//	public List<CkMetadata> fetchAllByType(String domainType,boolean fetchRootData) {
//		return fetchAll(fetchRootData).stream().filter(f->f.getDomainType().equalsIgnoreCase(domainType)).collect(Collectors.toList());
//	}
//
//	public List<CkMetadata> fetchByType(String domainType){
//		return this.metaDataRepository.findByDomainType(domainType);
//	}
//
//	public List<CkMetadata> fetchByDomainName(String domainName){
//		return this.metaDataRepository.findByDomainName(domainName);
//	}
//
//	public void persist(CkMetadata metaData) {
//		 metaDataRepository.save(metaData);
//		 onCacheChange(metaData);
//		 distributedCache.publishCacheEvent(SecurityContextUtils.getLob(),String.format("%s:%s:%s", CACHE_DOMAIN,metaData.getDomainName(),metaData.getDomainType()),
//					CacheOperationsConstant.DELETE,metaData);
//		 fetchAll();
//		 requestCache.clearAll();
//	}
//
//	public void merge(CkMetaData metaData) {
//		metaDataRepository.merge(metaData);
//		onCacheChange(metaData);
//		distributedCache.publishCacheEvent(SecurityContextUtils.getLob(),String.format("%s:%s:%s", CACHE_DOMAIN,metaData.getDomainName(),metaData.getDomainType()),
//					CacheOperationsConstant.DELETE,metaData);
//		fetchAll();
//		requestCache.clearAll();
//	}
//
//	public void clear() {
//		distributedCache.clearCache(SecurityContextUtils.getLob(), null, CACHE_DOMAIN);
//	}
//
//	/**
//	 * Save.
//	 *
//	 * @param cdmObject the cdm object
//	 * @return the meta data
//	 */
//	@Override
//	public CkMetadata save(CkMetadata cdmObject) {
//		CkMetadata metaData= null;
//		try {
//			//Cache clear
//			AppCacheManager.getInstance().clearCache(SecurityContextUtils.getLob(),SecurityContextUtils.getLob()+":"+cdmObject.getDomainName(),cdmObject.getDomainType());
//			clear();
//
//			metaData = super.save(cdmObject);
//			onCacheChange(metaData);
//			return metaData;
//		}finally {
//			if(metaData != null) {
//				distributedCache.publishCacheEvent(SecurityContextUtils.getLob(),String.format("%s:%s:%s", CACHE_DOMAIN,cdmObject.getDomainName(),cdmObject.getDomainType()),
//						CacheOperationsConstant.DELETE,metaData);
//			}
//		}
//	}
//
//
//	/**
//	 * Batch save.
//	 *
//	 * @param iterObj the iter obj
//	 * @return the list
//	 */
//	@Override
//	public List<CkMetadata> batchSave(Iterable<CkMetadata> iterObj) {
//		String lob= SecurityContextUtils.getLob();
//		try {
//			return super.batchSave(iterObj);
//		}finally {
//			iterObj.forEach(element->{
//				distributedCache.publishCacheEvent(lob,String.format("%s:%s:%s", CACHE_DOMAIN,element.getDomainName(),element.getDomainType()),
//						CacheOperationsConstant.DELETE,element);
//				onCacheChange(element);
//			});
//		}
//	}
//
//	/**
//	 * Delete metadata by value
//	 * @param domainName
//	 * @param domainType
//	 */
//	public void delete(String domainName, String domainType) throws NoSuchElementException {
//		CkMetadata metaData = metaDataRepository.deleteByValue(domainName, domainType);
//		onCacheChange(metaData);
//		distributedCache.publishCacheEvent(SecurityContextUtils.getLob(),String.format("%s:%s:%s", CACHE_DOMAIN, domainName, domainType),
//				CacheOperationsConstant.DELETE, metaData);
//		fetchAll();
//		requestCache.clearAll();
//	}
//
//
//	public List<CkMetadata> getAllProfileMetaDat() {
//		// domainName is hardcoded because this method is specially created to get the profile details from metaData
//		return this.metaDataRepository.findAll("profile");
//
//	}
//
//    public void isValidDomain(String domainName) {
//		if(!appConfigDomainNameSet.contains(domainName)){
//			throw new AccessDeniedException("You don't have access to create/update metadata with provided domainName ");
//		}
//    }
//
//
//	public void isValidPlayGroundDomain(String domainName,String domainType) {
//		if(!playgroudDomainName.containsKey(domainName) || !playgroudDomainName.get(domainName).equalsIgnoreCase(domainType)){
//			throw new AccessDeniedException("You don't have access to create/update metadata with provided domainName ");
//		}
//	}
}
