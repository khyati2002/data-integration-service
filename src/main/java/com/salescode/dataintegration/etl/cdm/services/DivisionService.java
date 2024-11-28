/*
 * Copyright (c) Applicate AI 2022. All rights reserved.
 *
 */
package com.salescode.dataintegration.etl.cdm.services;


import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.DivisionRepository;
import com.salescode.jooq.generated.tables.pojos.CkDivision;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.salescode.jooq.generated.Tables.CK_DIVISION;

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

//	private DistributedCache distributedCache;

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
	public DivisionService(DSLContext dslContext, DivisionRepository divisionRepository
						   //, DistributedCache distributedCache
						   ) {

		super(dslContext);
		this.divisionRepository = divisionRepository;
	//	this.distributedCache = distributedCache;
	}

	/** The Constant CACHE_DOMAIN. */
	private static final String CACHE_DOMAIN = "divisions";

	/**
	 * Find by division name.
	 *
	 * @param divisionName the division name
	 * @return the division
	 */
//	public List<Division> findByDivisionName(String divisionName) {
//		Assert.notNull(divisionName, "Invalid argument");
//		Collection<Division> divisions = findAllOrderByLevelAsc(true);
//		Function<String, List<Division>> function = division -> divisions.stream()
//				.filter(p -> p.getDivisionName().equalsIgnoreCase(division)).collect(Collectors.toList());
//
//		return (ObjectUtils.isNotEmpty(divisions)) ? function.apply(divisionName) : List.of();
//	}

	/**
	 * Find by division name and parent.
	 *
	 * @param divisionName the division name
	 * @param parent       the parent
	 * @return the division
	 */
//	public Division findByDivisionNameAndParent(String divisionName, String parent) {
//		Assert.notNull(divisionName, "Invalid argument");
//		Collection<Division> divisions = findAllOrderByLevelAsc(true);
//		BiFunction<String, String, Division> function = (division, s) -> divisions.stream().filter(p -> {
//
//			boolean b1 = StringUtils.equalsIgnoreCase(p.getDivisionName(), divisionName);
//			boolean b2 = StringUtils.equalsIgnoreCase(p.getParent(), parent);
//			return b1 && b2;
//		}).findFirst().orElse(null);
//
//		return (ObjectUtils.isNotEmpty(divisions)) ? function.apply(divisionName, parent) : null;
//	}

	/**
	 * Find all order by level asc.
	 *
	 * @param cache the cache
	 * @return the collection
	 */
//	public Collection<Division> findAllOrderByLevelAsc(boolean cache) {
//		String lob = SecurityContextUtils.getLob();
//		return (cache) ? distributedCache.withCache(lob, CACHE_DOMAIN, "division", mapdata -> {
//			Collection<Division> data = divisionRepository.findByOrderByLevelAsc();
//			if (data == null || data.isEmpty()) {
//				return null;
//			}
//			return data;
//		}) : divisionRepository.findByOrderByLevelAsc();
//	}

	/**
	 * Find by channel division order by level asc.
	 *
	 * @return the collection
	 */
	public Collection<CkDivision> findByChannelDivisionOrderByLevelAsc() {
//		String lob = SecurityContextUtils.getLob();
//		return distributedCache.withCache(lob, CACHE_DOMAIN, "channeldivision", mapdata -> {
			Collection<CkDivision> data = divisionRepository.findByChannelDivisionOrderByLevelAsc(true);
			if (data == null || data.isEmpty()) {
				return null;
			}
			return data;
//		});
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
//			throw new CustomRuntimeException(
//					"Channel division not found. [Hint : Make sure division data present in database]");
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
//	public boolean isChannelDivision(Set<String> divisionNames) {
//		List<Division> divisions = (List<Division>) findByChannelDivisionOrderByLevelAsc();
//		if (divisions == null || divisions.isEmpty()) {
//			throw new NullPointerException(
//					"Channel division not found. [Hint : Make sure division data present in database and atleast one division is configured as channel]");
//		}
//		boolean result = false;
//		for (Division division : divisions) {
//			for (String inputDivision : divisionNames) {
//				if (division.getDivisionName().equalsIgnoreCase(inputDivision) && division.isChannelDivision()) {
//					result = true;
//					break;
//				}
//			}
//		}
//		return result;
//	}

//	/**
//	 * Batch save.
//	 *
//	 * @param iterObj the iter obj
//	 * @return the list
//	 */
//	@Override
//	public List<Division> batchSave(Iterable<Division> iterObj) {
//		clearCache();
//		List<Division> divisions = StreamSupport.stream(iterObj.spliterator(), false).collect(Collectors.toList());
//		List<Division> saveddivisions = super.findAll();
//		if (!CollectionUtils.isEmpty(saveddivisions)) {
//			List<Division> diff = saveddivisions.stream().filter(f -> !divisions.contains(f))
//					.collect(Collectors.toList());
//			divisions.addAll(diff);
//		}
//		setLevel(divisions);
//		return super.batchSave(divisions);
//	}

	public boolean isChannelDivisionPresent() {
		List<CkDivision> divisions = (List<CkDivision>) findByChannelDivisionOrderByLevelAsc();
		if (divisions == null || divisions.isEmpty()) {
			logger.error("Channel division not found. Please ensure division data are present.");
			return false;
		}
		return true;
	}

	@Override
	protected Table<? extends Record> getTable() {
		return CK_DIVISION;
	}

//	/**
//	 * Save.
//	 *
//	 * @param division the division
//	 * @return the division
//	 */
//	@Override
//	public Division save(Division division) {
//		clearCache();
//		List<Division> divisions = new ArrayList<>();
//		divisions.add(division);
//		List<Division> saved = this.batchSave(divisions);
//		Optional<Division> div = saved.stream()
//				.filter(f -> StringUtils.equalsAnyIgnoreCase(f.getDivisionName(), division.getDivisionName())
//						&& StringUtils.equalsAnyIgnoreCase(f.getParent(), division.getParent()))
//				.findFirst();
//		return (div.isPresent()) ? div.get() : null;
//	}

	/**
	 * Delete.
	 *
	 * @param division the division
	 * @throws ValidationException 
	 */
//	public void delete(Division division) {
//		try {
//			if(isDivisionConnectedToAnyUser(division)) {
//				throw new ValidationException(ValidationResponseMessage.DIVISION_USEREXIST_VALIDATION_MSG, division.getDivisionName());
//			}
//			List<Division> pdivisions= getDivisionInParent(division);
//			if(!CollectionUtils.isEmpty(pdivisions)) {
//				List<String> users= new ArrayList<>();
//				pdivisions.forEach(elem->users.add(elem.getDivisionName()));
//				throw new ValidationException(ValidationResponseMessage.DIVISION_IS_PARENT_OF_OTHER_DIVISION, division.getDivisionName(),
//						StringUtils.join(users));
//			}
//			clearCache();
//			divisionRepository.delete(division);
//		}catch(ValidationException ex) {
//			throw new CustomRuntimeException(ex);
//		}
//	}
//
//	/**
//	 * Clear cache.
//	 */
//	public void clearCache() {
//		distributedCache.clearCache(SecurityContextUtils.getLob(), CACHE_DOMAIN);
//		(SpringContext.getBean(SupplierInfoService.class)).clearCache();
//	}
//
//	/**
//	 * Sets the level.
//	 *
//	 * @param existingDivisions the new level
//	 */
//	public void setLevel(List<Division> divisions) {
//		if (CollectionUtils.isEmpty(divisions)) {
//			logger.info("Cannot evaluate level for passed divisions. Parameter divisons found null or empty.");
//			return;
//		}
//		Map<Node, ArrayList<Node>> testTreeNode = new HashMap<>();
//		List<Node> testRoot = new ArrayList<>();
//		divisions.stream().forEach(div -> {
//			Node node = new Node();
//			node.setName(div.getDivisionName());
//			testTreeNode.put(node, new ArrayList<>());
//		});
//
//		for (Division div : divisions) {
//			Optional<Node> n = testTreeNode.keySet().stream().filter(f -> f.getName().equals(div.getDivisionName()))
//					.findFirst();
//			if (n.isPresent()) {
//				if(div.getParent() == null) {
//					continue;
//				}
//				if (!NA.equalsIgnoreCase(div.getParent())) {
//					Node node = new Node();
//					node.setName(div.getParent());
//					if (testTreeNode.get(node) == null) {
//						throw new IllegalArgumentException(
//								"Parent with name : '{}' not found. Make sure parent is part of master data",
//								div.getParent());
//					}
//					testTreeNode.get(node).add(n.get());
//					validate(testTreeNode,n.get(),node);
//				} else {
//					testRoot.add(n.get());
//				}
//			}
//		}
//		parseAndSetLevel(testRoot, divisions, testTreeNode);
//	}

	/**
	 * Sets the level
	 *
	 * @param n                 the n
	 * @param tree              the tree
	 * @param existingDivisions the existing divisions
	 * @return the int
	 */
//	private int setLevel(Node n, Map<Node, ArrayList<Node>> tree, List<Division> existingDivisions, int currentLevel) {
//		List<Node> childs = tree.get(n);
//		if (ObjectUtils.isNotEmpty(childs)) {
//			currentLevel++;
//			for (Node child : childs) {
//				setLevel(child, tree, existingDivisions,currentLevel);
//				int level = CUT * currentLevel;
//				if (logger.isDebugEnabled()) {
//					logger.debug(" ===== Node : {}, Level : {} ===== ", child.getName(), level);
//				}
//				existingDivisions.stream().filter(p -> p.getDivisionName().equals(child.getName()))
//				.forEach(m -> m.setLevel(level));
//			}
//		}
//
//		if (currentLevel >= threshold) {
//			SystemRuntimeException ex = new SystemRuntimeException(
//					"Division has surpassed max threshold value for generating levels. This may happen due to recursive division hierarchy.");
//			logger.error(ex.getLocalizedMessage(), ex);
//			throw ex;
//		}
//
//		return currentLevel;
//	}
//
//	/**
//	 * Validates the tree
//	 *
//	 * @param testTreeNode the test tree node
//	 * @param inNode the in node
//	 * @param node the node
//	 */
//	private void validate(Map<Node, ArrayList<Node>> testTreeNode, Node inNode, Node node) {
//		List<Node> p1nodes= testTreeNode.get(node);
//		List<Node> p2nodes= testTreeNode.get(inNode);
//		if((!CollectionUtils.isEmpty(p1nodes) && !CollectionUtils.isEmpty(p2nodes)) &&
//				(p1nodes.contains(inNode) && p2nodes.contains(node))) {
//			throw new IllegalArgumentException(
//					"Cyclic user hierarchy detected between {} and {}",
//					node.getName(), inNode.getName());
//		}
//	}
//
//	/**
//	 * Parses the and set level.
//	 *
//	 * @param testRoot the test root
//	 * @param divisions the divisions
//	 * @param testTreeNode the test tree node
//	 */
//	private void parseAndSetLevel(List<Node> testRoot, List<Division> divisions, Map<Node, ArrayList<Node>> testTreeNode) {
//		for (Node node : testRoot) {
//			int level= 1;
//			if (logger.isDebugEnabled()) {
//				logger.debug(" ===== Node : {}, Level : {} ===== ", node.getName(), level);
//			}
//			divisions.stream().filter(p -> p.getDivisionName().equals(node.getName())).forEach(m -> m.setLevel(level));
//			setLevel(node, testTreeNode, divisions,1);
//		}
//	}
//
//	/**
//	 * Checks if is division connected to any user.
//	 *
//	 * @param division the division
//	 * @return true, if is division connected to any user
//	 */
//	private boolean isDivisionConnectedToAnyUser(Division division) {
//		UserService userService= SpringContext.getBean(UserService.class);
//		Long count= userService.countUsersByDesignation(division.getDivisionName());
//		return (count > 0);
//	}
//
//	/**
//	 * Checks if is division is parent of any other division.
//	 *
//	 * @param division the division
//	 * @return true, if is division of any parent
//	 */
//	private List<Division> getDivisionInParent(Division division){
//		return divisionRepository.findByParent(division.getDivisionName());
//	}

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
