/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.HierarchyMetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;


@Service
public class HierarchyMetaDataService extends AbstractCDMService<CkHierarchyMetadata> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String CACHE_DOMAIN="hierarchymetadatas";

    @Autowired
    private HierarchyMetaDataRepository hierarchyMetaDataRepository;

    @Autowired
    private DistributedCache distributedCache;

    public HierarchyMetaDataService(HierarchyMetaDataRepository repository) {
        this.hierarchyMetaDataRepository=repository;
    }


    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId) {
        return findByImmediateParent(loginId,true);
    }

    public Collection<CkHierarchyMetadata> findByImmediateParent(String loginId, boolean cached) {
        Function<String,Collection<CkHierarchyMetadata>> function = (String lid)->{
            return hierarchyMetaDataRepository.findByImmediateParent(lid);
        };
        logger.debug("Find immediate Parent for->>>>>>>>>>>>:{}", loginId);
        return (cached) ? AppCacheManager.getInstance().withCache(CACHE_DOMAIN, loginId,function):function.apply(loginId);
    }

    public Collection<CkHierarchyMetadata> findByImmediateParent(List<String> loginId) {
        return hierarchyMetaDataRepository.findByImmediateParentIn(loginId);
    }

    public void clearCache(String lob, String loginId) {
        if(org.apache.commons.lang3.StringUtils.isNotBlank(loginId)) {
            AppCacheManager.getInstance().clearCache(lob, CACHE_DOMAIN, loginId);
            distributedCache.clearCache(lob, CACHE_DOMAIN, loginId);
        }
    }

    @Override
    public CkHierarchyMetadata save(CkHierarchyMetadata cdmObject) {
        //List<HierarchyMetaData> dbData=(List<HierarchyMetaData>) findByImmediateParent(cdmObject.getImmediateParent(),true);
        //if(dbData != null && !dbData.isEmpty()) {
        String lob= SecurityContextUtils.getLob();
        //distributedCache.clearCache(lob, CACHE_DOMAIN, cdmObject.getImmediateParent());
        //distributedCache.clearCache(lob, UserService.CACHE_DOMAIN, cdmObject.getImmediateParent());
        //AppCacheManager.getInstance().removeByDomain(CACHE_DOMAIN,cdmObject.getHierarchy());
        clearCache(lob,cdmObject.getParent());
        //}
        CkHierarchyMetadata saved= super.save(cdmObject);
        if(saved != null) {
            clearCache(lob,saved.getParent());
        }
        return saved;
    }

    public List<CkHierarchyMetadata> findParentThroughUserLoginId(String loginid){
        return hierarchyMetaDataRepository.findMyHierarchy(loginid);
    }

    public void deleteHierarchyMetaData(String loginid) {
        hierarchyMetaDataRepository.deleteHierarchyMetaData(loginid);
    }


    public CkHierarchyMetadata findByHierarchy(String hierarchy) {
        return hierarchyMetaDataRepository.findByHierarchy(hierarchy);
    }


}
