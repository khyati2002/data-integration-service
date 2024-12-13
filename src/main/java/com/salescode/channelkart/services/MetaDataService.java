/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;

import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.repository.MetaDataRepository;

import org.springframework.stereotype.Service;

@Service
public class MetaDataService extends AbstractCDMService<MetaData> {

    MetaDataRepository metaDataRepository;

    public MetaDataService(MetaDataRepository repository) {
        super(repository);
        this.metaDataRepository = repository;
    }


    public MetaData fetchByValueFromDB(String domainName, String domainType) {
        return this.metaDataRepository.findByDomainNameAndDomainType(domainName, domainType).orElse(null);
    }

    public MetaData fetchByValue(String domainName, String domainType) {
        return fetchByValue(domainName, domainType, false);
    }

    public MetaData fetchByValue(String domainName, String domainType, boolean cached) {
        //return AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob()+":"+domainName,domainType,(s)->fetchByValueFromDB(domainName,domainType));
       // return fetchByValueFromDB(domainName, domainType);
        //return fetchAll().stream().filter(f->f.getDomainName().equalsIgnoreCase(domainName) && f.getDomainType().equalsIgnoreCase(domainType)).findFirst().orElse(null);
      return fetchByValueFromDB(domainName,domainType);
    }


}
