/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.channelkart.services;


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



	/** The cut. */
	private static final int CUT = 1;

	/** The threshold. */
	private static int threshold = 300;

	/** The Constant NA. */
	private static final String NA = "none";

    /**
	 * Instantiates a new division service.
	 *
	 * @param divisionRepository the repository
	 */
	public DivisionService(DivisionRepository divisionRepository
			//, DistributedCache distributedCache
						   ) {

		super();
		this.divisionRepository = divisionRepository;
	//	this.distributedCache = distributedCache;
	}

	/** The Constant CACHE_DOMAIN. */
	private static final String CACHE_DOMAIN = "divisions";

	/**
	 * Find by channel division order by level asc.
	 *
	 * @return the collection
	 */
	public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc() {


			Collection<CkDivision> data = divisionRepository.findByChannelDivisionOrderByLevelAsc(true);
			if (data == null || data.isEmpty()) {
				return null;
			}
			return data;

	}

	/**
	 * Checks if is channel division.
	 *
	 * @param divisionName the division name
	 * @return true, if is channel division
	 */
	public boolean isChannelDivision(String divisionName) {
		List<CkDivision> divisions = (List<CkDivision>) findByChannelDivisionOrderByLevelAsc();
		if (divisions == null || divisions.isEmpty()) {


		}
		Optional<CkDivision> division = divisions.parallelStream().filter(
				element -> element.getDivisionName().equalsIgnoreCase(divisionName) && element.getChannelDivision())
				.findAny();
		return division.isPresent();
	}

	/**
	 * Checks if is channel division.
	 *
	 * @param divisionNames the division names
	 * @return true, if is channel division
	 */


	public boolean isChannelDivisionPresent() {
		List<CkDivision> divisions = (List<CkDivision>) findByChannelDivisionOrderByLevelAsc();
		if (divisions == null || divisions.isEmpty()) {
			logger.error("Channel division not found. Please ensure division data are present.");
			return false;
		}
		return true;
	}

	/**
	 * The Class Node.
	 *
	 * @author Manish Srivastava
	 * @since Dec 2021
	 */
	private static class Node {

		/** The parent. */
		private Node parent;

		/** The name. */
		private String name;

		/**
		 * Gets the parent.
		 *
		 * @return the parent
		 */
		@SuppressWarnings("unused")
		public Node getParent() {
			return parent;
		}

		/**
		 * Sets the parent.
		 *
		 * @param parent the new parent
		 */
		@SuppressWarnings("unused")
		public void setParent(Node parent) {
			this.parent = parent;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the name.
		 *
		 * @param name the new name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Node other = (Node) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "Node [name=" + name + "]";
		}

	}	

}
