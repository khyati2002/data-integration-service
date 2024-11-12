/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.dataintegration.etl.cdm.services;

import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.repository.HierarchyMetaDataRepository;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * The class HierarchyMetaDataService.
 *
 * @author Manish Srivastava
 * @since  2020
 */
@Service
public class HierarchyMetaDataService extends AbstractCDMService<HierarchyMetaData>  {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private static final String CACHE_DOMAIN="hierarchymetadatas";
	
	/** The hierarchy meta data repository. */
	@Autowired	
	private HierarchyMetaDataRepository hierarchyMetaDataRepository;
	
	@Autowired
	private DistributedCache distributedCache;
	
	/**
	 * Instantiates a new hierarchy meta data service.
	 *
	 * @param repository the repository
	 */
	public HierarchyMetaDataService(HierarchyMetaDataRepository repository) {
		super(repository);
		this.hierarchyMetaDataRepository=repository;
	}

    /**
     * Find by immediate parent.
     *
     * @param loginId the login id
     * @return the hierarchy meta data
     */
    public Collection<HierarchyMetaData> findByImmediateParent(String loginId) {
        return findByImmediateParent(loginId,true);
    }
    
    /**
     * Find by immdiate parent cached.
     *
     * @param loginId the login id
     * @param cached the cached
     * @return the hierarchy meta data
     */
    public Collection<HierarchyMetaData> findByImmediateParent(String loginId, boolean cached) {
    	Function<String,Collection<HierarchyMetaData>> function = (String lid)->{
			return hierarchyMetaDataRepository.findByImmediateParent(lid);
		};
    	logger.debug("Find immediate Parent for->>>>>>>>>>>>:{}", loginId);
		return (cached) ? AppCacheManager.getInstance().withCache(CACHE_DOMAIN, loginId,function):function.apply(loginId);
    }
    
    public Collection<HierarchyMetaData> findByImmediateParent(List<String> loginId) {
		return hierarchyMetaDataRepository.findByImmediateParentIn(loginId);
    }
    
    @Override
    public HierarchyMetaData save(HierarchyMetaData cdmObject) {
    	//List<HierarchyMetaData> dbData=(List<HierarchyMetaData>) findByImmediateParent(cdmObject.getImmediateParent(),true);
    	//if(dbData != null && !dbData.isEmpty()) {
    		String lob= SecurityContextUtils.getLob();
    		//distributedCache.clearCache(lob, CACHE_DOMAIN, cdmObject.getImmediateParent());
    		//distributedCache.clearCache(lob, UserService.CACHE_DOMAIN, cdmObject.getImmediateParent());
    		//AppCacheManager.getInstance().removeByDomain(CACHE_DOMAIN,cdmObject.getHierarchy());
    		clearCache(lob,cdmObject.getImmediateParent());
    	//}
    	HierarchyMetaData saved= super.save(cdmObject);
    	if(saved != null) {
    		clearCache(lob,saved.getImmediateParent());
    	}
    	return saved;
    }

	@Override
	public List<HierarchyMetaData> batchSave(Iterable<HierarchyMetaData> iterObj) {
    	return batchSave(iterObj,true);
	}
	public List<HierarchyMetaData> batchSave(Iterable<HierarchyMetaData> iterObj,boolean clearCache) {
    	String lob= SecurityContextUtils.getLob();

    	if(clearCache) {
				iterObj.forEach(element -> {
					if (element != null) {
						//AppCacheManager.getInstance().removeByDomain(CACHE_DOMAIN,element.getHierarchy());
						//distributedCache.clearCache(lob, CACHE_DOMAIN, element.getImmediateParent());
						//distributedCache.clearCache(lob, UserService.CACHE_DOMAIN, element.getImmediateParent());
						clearCache(lob, element.getImmediateParent());
					}
				});
			}
    	List<HierarchyMetaData> saved= super.batchSave(iterObj);
    	if(saved != null && clearCache) {
			saved.forEach(element->clearCache(lob,element.getImmediateParent()));
    	}
    	return saved;
    }
    
    /**
     * Clear cache.
     *
     * @param lob the lob
     * @param loginId the login id
     */
    public void clearCache(String lob, String loginId) {
    	if(StringUtils.isNotBlank(loginId)) {
    		AppCacheManager.getInstance().clearCache(lob, CACHE_DOMAIN, loginId);
    		distributedCache.clearCache(lob, CACHE_DOMAIN, loginId);
    	}
    }
    
    /**
     * Update hierarchy.
     */
    public void updateHierarchy() {
    	hierarchyMetaDataRepository.updateHierarchy();
    }

    
    /**
     * Find parent through user login id.
     *
     * @param loginid the loginid
     * @return the list
     */
    public List<HierarchyMetaData> findParentThroughUserLoginId(String loginid){
    	return hierarchyMetaDataRepository.findMyHierarchy(loginid);
    }
    
    public void deleteHierarchyMetaData(String loginid) {
    	hierarchyMetaDataRepository.deleteHierarchyMetaData(loginid);
    }
    
    /**
     * Find by hierarchy.
     *
     * @param hierarchy the hierarchy
     * @return the hierarchy meta data
     */
    
    @Transactional(propagation= Propagation.REQUIRED,readOnly=true)
    public HierarchyMetaData findByHierarchy(String hierarchy) {
    	return hierarchyMetaDataRepository.findByHierarchy(hierarchy);
    }
    
    @Transactional(propagation= Propagation.REQUIRED)
	public void deleteHierarchyMetaData(Collection<String> hierarchy) {
		hierarchyMetaDataRepository.deleteByHierarchyIn(hierarchy);
	}
    
    @Transactional(propagation= Propagation.REQUIRED)
   	public void deleteByHierarchy(String hierarchy) {
    	if(StringUtils.isNotEmpty(hierarchy)) {
    		clearCache(SecurityContextUtils.getLob(),hierarchy.split(" > ")[0]);
    	}
   		hierarchyMetaDataRepository.deleteByHierarchy(hierarchy);
   	}
    
    @Transactional(propagation= Propagation.REQUIRED)
    public void updateLocationHierarchy() {
    	hierarchyMetaDataRepository.updateLocationHierarchy();
    }
    
    public List<HierarchyMetaData> findByParentMatchByHierarchy(String loginId, String hierarchyUser){
    	return hierarchyMetaDataRepository.findByParentMatchByHierarchy(loginId, hierarchyUser);
    	}
	
}
