/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.channelkart.repository;

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


	public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc(boolean isChannelDivision);

	
}
