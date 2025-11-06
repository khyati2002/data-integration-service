package com.applicate.services.channelkart.repository;

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
}