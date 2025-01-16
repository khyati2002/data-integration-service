/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.transformers;

import com.applicate.services.channelkart.cache.AllLOBRouter;
import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.registry.AbstractRegistry;
import com.applicate.services.channelkart.services.SpringContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The class TransformerRegistry.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
public class TransformerRegistry extends AbstractRegistry<TransformerInfo> {

	/** The Constant INSTANCE. */
	public static final TransformerRegistry INSTANCE = new TransformerRegistry();

	/** The Constant EMPTY_TRANSFORMERS. */
	private static final List<TransformerInfo> EMPTY_TRANSFORMERS=Collections
            .unmodifiableList(new ArrayList<>());

	private DistributedCache distributedCache = SpringContext.getBean(DistributedCache.class);
	
	private static final String CACHE_DOMAIN = "transformer";
	
	/**
	 * Instantiates a new transformer registry.
	 */
	private TransformerRegistry() {
	}

	/**
	 * Adds the.
	 *
	 * @param transformerInfo the transformer info
	 */
	@Override
	public void add(TransformerInfo transformerInfo) {
		synchronized (transformerInfo.getLob().intern()) {
			List<TransformerInfo> rules = (List<TransformerInfo>) distributedCache.get(transformerInfo.getLob(), null, CACHE_DOMAIN, false);
			if (rules == null) {
				rules = new ArrayList<>();
			}
			rules.add(transformerInfo);
			clear(transformerInfo.getLob());
			distributedCache.put(transformerInfo.getLob(), null, CACHE_DOMAIN, rules, false);
		}
	}


	/**
	 * Gets the.
	 *
	 * @param lob the lob
	 * @return the list
	 */
	@Override
	public List<TransformerInfo> get(String lob) {
		List<TransformerInfo> transformer = AppCacheManager
				.getInstance().withCache(lob,CACHE_DOMAIN,(sk)-> distributedCache.withCache(lob, null, CACHE_DOMAIN,
				(s)-> AllLOBRouter.loadAll(TransformerInfo.class,(k) -> k.equalsIgnoreCase(lob)).stream().map(e->(TransformerInfo)e).collect(Collectors.toList())));
		return lob!=null && transformer != null ?transformer:EMPTY_TRANSFORMERS;
	}

	/**
	 * Gets the.
	 *
	 * @param lob the lob
	 * @param type the type
	 * @return the list
	 */
	@Override
	public List<TransformerInfo> get(String lob, String type) {
		return get(lob,(rule)->rule.getType().equalsIgnoreCase(type) && rule.isEnabled());
	}
	
	@Override
	public void addAll(String lob,List<TransformerInfo> transformer) {
		synchronized (lob.intern()) {
			clear(lob);
			distributedCache.put(lob, null, CACHE_DOMAIN, transformer, false);
		}
	}

	/**
	 * Load all transformers.
	 */
	@Override
	public void loadAll(Predicate<String> predicate,boolean overrideOld) {
		AllLOBRouter.loadAll(TransformerInfo.class,predicate).stream().map(e->(TransformerInfo)e).collect(Collectors.groupingBy(TransformerInfo::getLob)).
		entrySet().stream().forEach(e->{
			if(distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false)==null ||overrideOld)
				addAll(e.getKey(),e.getValue());
		});
	}

	@Override
	public void clear(String lob) {
		distributedCache.clearCache(lob, null, CACHE_DOMAIN);
	}
	

	/**
	 * Gets transformer by name.
	 *
	 * @param lob the lob
	 * @param name the name
	 * @return the by name
	 */
	public Optional<TransformerInfo> getByName(String lob, String name) {
		List<TransformerInfo> transformers=  get(lob,rule->StringUtils.equalsIgnoreCase(rule.getName(), name) && rule.isEnabled());
		if(ObjectUtils.isNotEmpty(transformers)) {
			return Optional.of(transformers.get(0));
		}
		return Optional.empty();
	}

}
