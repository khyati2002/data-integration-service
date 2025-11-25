/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.repository.MetaDataRepository;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.jooq.DSLContext;

import java.util.List;

public class MetaDataService extends AbstractCDMService<Metadata> {

	private static MetaDataRepository metaDataRepository;

	public MetaDataService() {
		if(metaDataRepository == null) {
			metaDataRepository = new MetaDataRepository(getDslContext());
		}
	}

	@Cacheable(cacheName = "dataintegration-metadata1")

	public Metadata fetchByValueFromDB(String domainName,String domainType) {
		return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
	}
	public Metadata fetchByValue(String domainName, String domainType, boolean cached) {

		return this.fetchByValueFromDB(domainName, domainType);
	}
	public List<Metadata> O(String domainName){
		return metaDataRepository.findByDomainName(domainName);
	}

	@Cacheable(cacheName = "dataintegration-metadata")
	public Metadata fetchByValue(String domainName,String domainType) {
		return fetchByValueFromDB(domainName,domainType);
	}

}
