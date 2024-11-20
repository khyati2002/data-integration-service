/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.dataintegration.etl.cdm.repository;

//import com.applicate.services.channelkart.models.Division;
import com.salescode.jooq.generated.tables.pojos.CkDivision;
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
public interface DivisionRepository {

	/**
	 * Find by division name.
	 *
	 * @param divisionName the division name
	 * @return the division
	 */
	public CkDivision findByDivisionName(String divisionName);
	
	/**
	 * Find by order by level asc.
	 *
	 * @return the collection
	 */
	public Collection<CkDivision> findByOrderByLevelAsc();
	
	/**
	 * Find by channel division order by level asc.
	 *
	 * @param isChannelDivision the is channel division
	 * @return the collection
	 */
	public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc(boolean isChannelDivision);
	
	/**
	 * Find by user hierarchy parent.
	 *
	 * @param divisionName the division name
	 * @return the list
	 */
	public List<CkDivision> findByParent(String divisionName);
	
}
