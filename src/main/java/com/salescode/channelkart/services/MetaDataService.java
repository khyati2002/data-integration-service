/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;

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
//import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.MetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.springframework.stereotype.Service;

@Service
public class MetaDataService extends AbstractCDMService<CkMetadata> {

	MetaDataRepository metaDataRepository;

	public MetaDataService(MetaDataRepository repository)
    {
        this.metaDataRepository = repository;
	}

	public CkMetadata fetchByValueFromDB(String domainName,String domainType) {
		return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
	}

	public CkMetadata fetchByValue(String domainName,String domainType) {
		return fetchByValue(domainName, domainType, false);
	}

	public CkMetadata fetchByValue(String domainName,String domainType,boolean cached) {
		//return AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob()+":"+domainName,domainType,(s)->fetchByValueFromDB(domainName,domainType));
        return fetchByValueFromDB(domainName,domainType);
		//return fetchAll().stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName) && f.getDomainType().equalsIgnoreCase(domainType)).findFirst().orElse(null);
	}

}
