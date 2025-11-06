package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.SupplierMetaData;
import static com.salescode.dim.jooq.generated.Tables.CK_SUPPLIER_METADATA;
import org.jooq.DSLContext;

/**
 * jOOQ-based repository for accessing SupplierMetadata.
 * This is now a class, not an interface, and uses DSLContext directly.
 */
public class SupplierMetaDataRepository {

    private final DSLContext dsl;

    /**
     * Constructs the repository with a jOOQ DSLContext.
     * @param dsl The DSLContext provided by the service.
     */
    public SupplierMetaDataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * ADDED: Finds a SupplierMetadata entity by its primary key (ID).
     *
     * @param id The ID of the supplier.
     * @return The SupplierMetadata POJO object or null if not found.
     */
    public SupplierMetadata findById(String id) {
        return dsl.selectFrom(CK_SUPPLIER_METADATA)
                .where(CK_SUPPLIER_METADATA.ID.eq(id))
                .fetchOneInto(SupplierMetadata.class);
    }
}