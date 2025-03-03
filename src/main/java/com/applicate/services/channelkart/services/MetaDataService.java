/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.repository.MetaDataRepository;

import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.jooq.DSLContext;

public class MetaDataService extends AbstractCDMService<Metadata> {

	private static MetaDataRepository metaDataRepository;

	public MetaDataService(DSLContext dsl) {
		super(dsl);
		metaDataRepository = new MetaDataRepository(dsl);
	}


	public Metadata fetchByValueFromDB(String domainName,String domainType) {
		return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
	}

	public Metadata fetchByValue(String domainName,String domainType) {
		return fetchByValueFromDB(domainName,domainType);
	}


	@Override
	public Metadata save(Metadata cdmObject) {
		return null;
	}
}
