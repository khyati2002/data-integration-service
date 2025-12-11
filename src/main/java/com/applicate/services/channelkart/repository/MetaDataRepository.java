package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.salescode.dim.jooq.generated.Tables.CK_METADATA;


public class MetaDataRepository {

    private final DSLContext dsl;

    public MetaDataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Metadata> findByDomainNameAndDomainType(String domainName, String domainType){
        return dsl.selectFrom(CK_METADATA) // Replace CK_METADATA with your actual jOOQ table
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .fetchOptionalInto(Metadata.class); // Map result to CkMetadata POJO
    }

}
