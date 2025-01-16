/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;



import com.applicate.services.channelkart.models.UserParent;
import com.applicate.services.channelkart.repository.UserParentRepository;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Class UserParentService.
 *
 * @author Manish Srivastava
 * @since  Jun 2020
 */
@Service
public class UserParentService extends AbstractCDMService<UserParent> {

	/**
	 * Instantiates a new user parent service.
	 *
	 * @param userParentRepository the user parent repository
	 */
	public UserParentService(UserParentRepository userParentRepository) {
		super(userParentRepository);
		this.userParentRepository =  userParentRepository;
	}

	/** The user parent repository. */
	private UserParentRepository userParentRepository;

	/**
	 * Delete by user login id.
	 *
	 * @param loginid the loginid
	 */
	@Transactional(propagation= Propagation.REQUIRED)
	public void deleteByUserLoginId(String loginid) {
		userParentRepository.deleteByUserLoginId(loginid);
	}

	/**
	 * Delete by user login id in.
	 *
	 * @param loginids the loginids
	 */
	@Transactional(propagation= Propagation.REQUIRED)
	public void deleteByUserLoginIdIn(Collection<String> loginids) {
		userParentRepository.deleteByUserLoginIdIn(loginids);
	}


	/**
	 * Fetch all user parent records for provided parent list
	 * @param parentList the list of users loginId whose children user-parent records are to be fetched
	 * @return the list of user-parent records
	 */
	public List<UserParent> findByParentIn(List<String> parentList){
		if(CollectionUtils.isNotEmpty(parentList)){
			return userParentRepository.findByParentIn(parentList);
		}
		return new ArrayList<>();
	}

	/**
	 * Find by user login id.
	 *
	 * @param loginId the login id
	 * @return the list
	 */
	public List<UserParent> findByUserLoginId(String loginId) {
		return userParentRepository.findByUserLoginId(loginId);
	}

}
