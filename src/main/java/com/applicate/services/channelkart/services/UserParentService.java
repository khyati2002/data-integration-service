/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.repository.UserParentRepository;
import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import org.apache.commons.collections.CollectionUtils;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserParentService extends AbstractCDMService<UserParent> {

	private static UserParentRepository userParentRepository;
	public UserParentService(DSLContext dsl) {
		super(dsl);
		userParentRepository = new UserParentRepository(dsl);
	}


	public void deleteByUserLoginId(String loginid) {
		userParentRepository.deleteByUserLoginId(loginid);
	}


	public void deleteByUserLoginIdIn(Collection<String> loginids) {
		userParentRepository.deleteByUserLoginIdIn(loginids);
	}


	public List<UserParent> findByParentIn(List<String> parentList){
		if(CollectionUtils.isNotEmpty(parentList)){
			return userParentRepository.findByParentIn(parentList);
		}
		return new ArrayList<>();
	}

	public List<UserParent> findByUserLoginId(String loginId) {
		return userParentRepository.findByUserLoginId(loginId);
	}

	@Override
	public List<UserParent> batchSave(List<UserParent> cdmObject) {
		return List.of();
	}
}
