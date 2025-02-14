/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.MetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetaDataService extends AbstractCDMService<CkMetadata> {

	MetaDataRepository metaDataRepository;

	@Autowired
	private DistributedCache distributedCache;

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
		return AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob() + ":" + domainName, domainType,
				(s) -> distributedCache.withCache(SecurityContextUtils.getLob(), SecurityContextUtils.getLob() + ":" + domainName, domainType, (r) -> fetchByValueFromDB(domainName, domainType)));
		//return fetchAll().stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName) && f.getDomainType().equalsIgnoreCase(domainType)).findFirst().orElse(null);
	}

}
