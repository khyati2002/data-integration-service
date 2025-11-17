/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.repository.GenericObjectRepository;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;

public class GenericObjectService extends AbstractCDMService<GenericObject> {

	private static GenericObjectRepository genericObjectRepository;

	public GenericObjectService() {
		if(genericObjectRepository == null) {
			genericObjectRepository = new GenericObjectRepository(getDslContext());
		}
	}

	public GenericObject fetchByValueFromDB(String name,String key2) {
		return this.genericObjectRepository.findByNameAndKey2AndKey3(name, key2).orElse(null);
	}

	@Cacheable
	public GenericObject fetchByValue(String name,String key2) {
		return fetchByValueFromDB(name,key2);
	}

}
