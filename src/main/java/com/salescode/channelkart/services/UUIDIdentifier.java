/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.EntityUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * The class UUIDIdentifier.
 *
 * @author Manish Srivastava
 * @since  Apr 2020
 */
public class UUIDIdentifier implements IdentifierGenerator {

	/**
	 * Generate.
	 *
	 * @param session the session
	 * @param object the object
	 * @return the serializable
	 * @throws HibernateException the hibernate exception
	 */
	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		EntityUtils bean = SpringContext.getBean(EntityUtils.class);
		if(object instanceof CommonDataModel) {
			CommonDataModel cdm = (CommonDataModel)object;
			if(cdm.getId()!=null)
				return cdm.getId();
			return bean.generateId(cdm);
		}
		return UUID.randomUUID().toString();
	}
}