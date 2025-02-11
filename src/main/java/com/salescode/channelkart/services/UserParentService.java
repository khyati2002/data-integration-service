/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;



import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.UserParentRepository;
import com.salescode.jooq.generated.tables.pojos.CkUserParent;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Service
public class UserParentService extends AbstractCDMService<CkUserParent> {

	public UserParentService(UserParentRepository userParentRepository) {
		this.userParentRepository =  userParentRepository;
	}


	private UserParentRepository userParentRepository;


	public void deleteByUserLoginId(String loginid) {
		userParentRepository.deleteByUserLoginId(loginid);
	}


	public void deleteByUserLoginIdIn(Collection<String> loginids) {
		userParentRepository.deleteByUserLoginIdIn(loginids);
	}


	public List<CkUserParent> findByParentIn(List<String> parentList){
		if(CollectionUtils.isNotEmpty(parentList)){
			return userParentRepository.findByParentIn(parentList);
		}
		return new ArrayList<>();
	}

	public List<CkUserParent> findByUserLoginId(String loginId) {
		return userParentRepository.findByUserLoginId(loginId);
	}

}
