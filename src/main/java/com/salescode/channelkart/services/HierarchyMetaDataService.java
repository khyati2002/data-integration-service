/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.repository.HierarchyMetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class HierarchyMetaDataService extends AbstractCDMService<CkHierarchyMetadata> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String CACHE_DOMAIN="hierarchymetadatas";

    /** The hierarchy meta data repository. */
    @Autowired
    private HierarchyMetaDataRepository hierarchyMetaDataRepository;

    @Autowired


    public HierarchyMetaDataService(HierarchyMetaDataRepository repository) {
        this.hierarchyMetaDataRepository=repository;
    }

    /**
     * Instantiates a new hierarchy meta data service.
     *
     * @param repository the repository
     */


    /**
     * Find by immediate parent.
     *
     * @param loginId the login id
     * @return the hierarchy meta data
     */
    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId) {
        return findByImmediateParent(loginId,true);
    }








    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId, boolean cached) {
        Function<String,Collection<CkHierarchyMetadata>> function = (String lid)->{
            return hierarchyMetaDataRepository.findByImmediateParent(lid);
        };
        logger.debug("Find immediate Parent for->>>>>>>>>>>>:{}", loginId);
       // return (cached) ? AppCacheManager.getInstance().withCache(CACHE_DOMAIN, loginId,function):function.apply(loginId);
       return function.apply(loginId);
    }

    public Collection<CkHierarchyMetadata> findByImmediateParent(List<String> loginId) {
        return hierarchyMetaDataRepository.findByImmediateParentIn(loginId);
    }


    /**
     * Find parent through user login id.
     *
     * @param loginid the loginid
     * @return the list
     */
    public List<CkHierarchyMetadata> findParentThroughUserLoginId(String loginid){
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


    public CkHierarchyMetadata findByHierarchy(String hierarchy) {
        return hierarchyMetaDataRepository.findByHierarchy(hierarchy);
    }

}
