/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.channelkart.repository;


import com.salescode.channelkart.models.Division;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * The interface DivisionRepository.
 *
 * @author Manish Srivastava
 * @since  July 2020
 */
@Repository
public interface DivisionRepository extends CommonJpaRepository<Division, String>{

	/**
	 * Find by division name.
	 *
	 * @param divisionName the division name
	 * @return the division
	 */
	public Division findByDivisionName(String divisionName);
	
	/**
	 * Find by order by level asc.
	 *
	 * @return the collection
	 */
	public Collection<Division> findByOrderByLevelAsc();
	
	/**
	 * Find by channel division order by level asc.
	 *
	 * @param isChannelDivision the is channel division
	 * @return the collection
	 */
	public Collection<Division> findByChannelDivisionOrderByLevelAsc(boolean isChannelDivision);
	
	/**
	 * Find by user hierarchy parent.
	 *
	 * @param divisionName the division name
	 * @return the list
	 */
	public List<Division> findByParent(String divisionName);
	
}
