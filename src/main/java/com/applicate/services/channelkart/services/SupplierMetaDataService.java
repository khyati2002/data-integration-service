package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.SupplierMetaData;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.applicate.services.channelkart.repository.SupplierMetaDataRepository;
import com.salescode.dim.cache.Cacheable;

public class SupplierMetaDataService extends AbstractCDMService<SupplierMetaData>  {

    private final SupplierMetaDataRepository repository;
    public SupplierMetaDataService() {
        repository = new SupplierMetaDataRepository(getDslContext());
    }

    /**
     * Finds a SupplierMetadata entity by its primary key (ID).
     * Wraps the repository method and adds caching.
     *
     * @param id The ID of the supplier.
     * @return The SupplierMetaData impl object or null if not found.
     */
    @Cacheable(cacheName = "dataintegration-suppliermetadata")
    public SupplierMetaData findById(String id) {
        SupplierMetadata pojo = repository.findById(id);
        if(pojo == null){
            return null;
        }
        return SupplierMetaData.of(pojo);
    }
}