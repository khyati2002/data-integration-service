/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.DivisionRepository;
import com.salescode.jooq.generated.tables.pojos.CkDivision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The class DivisionService.
 *
 * @author Manish Srivastava
 * @since  July 2020
 * @version 1.2
 */
@Service
public class DivisionService extends AbstractCDMService<CkDivision> {

	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(DivisionService.class);
  
	/** The repository. */
	private DivisionRepository divisionRepository;

	private DistributedCache distributedCache;

	public DivisionService(DivisionRepository divisionRepository, DistributedCache distributedCache) {
		this.divisionRepository = divisionRepository;
		this.distributedCache = distributedCache;
	}

	private static final String CACHE_DOMAIN = "divisions";

	public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc() {
		String lob = SecurityContextUtils.getLob();
		return distributedCache.withCache(lob, CACHE_DOMAIN, "channeldivision", mapdata -> {
			Collection<CkDivision> data = divisionRepository.findByChannelDivisionOrderByLevelAsc(true);
			if (data == null || data.isEmpty()) {
				return null;
			}
			return data;
		});
	}

	public boolean isChannelDivision(String divisionName) {
		List<CkDivision> divisions = (List<CkDivision>) findByChannelDivisionOrderByLevelAsc();
		if (divisions == null || divisions.isEmpty()) {
			throw new CustomRuntimeException(
					"Channel division not found. [Hint : Make sure division data present in database]");
		}
		Optional<CkDivision> division = divisions.parallelStream().filter(
				element -> element.getDivisionName().equalsIgnoreCase(divisionName) && element.getChannelDivision())
				.findAny();
		return division.isPresent();
	}


	public boolean isChannelDivisionPresent() {
		List<CkDivision> divisions = (List<CkDivision>) findByChannelDivisionOrderByLevelAsc();
		if (divisions == null || divisions.isEmpty()) {
			logger.error("Channel division not found. Please ensure division data are present.");
			return false;
		}
		return true;
	}

}
