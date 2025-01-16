/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.repository.MetaDataRepository;

import com.applicate.services.channelkart.security.SecurityContextUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetaDataService extends AbstractCDMService<MetaData> {

    MetaDataRepository metaDataRepository;

    private DistributedCache distributedCache;


    public MetaDataService(MetaDataRepository repository, DistributedCache distributedCache) {
        super(repository);
        this.metaDataRepository = repository;
        this.distributedCache = distributedCache;
    }


    public MetaData fetchByValueFromDB(String domainName, String domainType) {
        return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
    }

    public MetaData fetchByValue(String domainName, String domainType) {
        return fetchByValue(domainName, domainType, false);
    }

    public MetaData fetchByValue(String domainName,String domainType,boolean cached) {
        return AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob() + ":" + domainName, domainType,
                (s) -> distributedCache.withCache(SecurityContextUtils.getLob(), SecurityContextUtils.getLob() + ":" + domainName, domainType, (r) -> fetchByValueFromDB(domainName, domainType)));
        //return fetchAll().stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName) && f.getDomainType().equalsIgnoreCase(domainType)).findFirst().orElse(null);
    }


    public List<MetaData> getAllProfileMetaDat() {
        // domainName is hardcoded because this method is specially created to get the profile details from metaData
        return this.metaDataRepository.findAll("profile");

    }

}
