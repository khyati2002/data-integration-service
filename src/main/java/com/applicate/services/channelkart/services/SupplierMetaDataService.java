package com.applicate.services.channelkart.services;

import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.SupplierMetaData;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.repository.SupplierMetaDataRepository;
import static com.salescode.dim.jooq.generated.Tables.CK_SUPPLIER_METADATA;

public class SupplierMetaDataService extends AbstractCDMService<SupplierMetaData>  {

    private static SupplierMetaDataRepository repository;
    public SupplierMetaDataService() {
        repository = new SupplierMetaDataRepository(getDslContext());
    }

    /**
     * ADDED: Finds a SupplierMetadata entity by its primary key (ID).
     *
     * @param id The ID of the supplier.
     * @return The SupplierMetadata POJO object or null if not found.
     */
    @Cacheable(cacheName = "dataintegration-suppliermetadata")
    public SupplierMetadata findById(String id) {
        return getDslContext().selectFrom(CK_SUPPLIER_METADATA)
                .where(CK_SUPPLIER_METADATA.ID.eq(id))
                .fetchOneInto(SupplierMetadata.class);
    }
}