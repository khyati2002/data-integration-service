package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.CategoryInfoRepository;
import com.salescode.dim.jooq.impl.EntityParentMapping;
import com.applicate.services.channelkart.repository.EntityParentMappingRepository;

public class EntityParentMappingService extends AbstractCDMService<EntityParentMapping> {

    private EntityParentMappingRepository entityParentMappingRepository;

    public EntityParentMappingService() {
        super();
        this.entityParentMappingRepository = new EntityParentMappingRepository(getDslContext());
    }
}