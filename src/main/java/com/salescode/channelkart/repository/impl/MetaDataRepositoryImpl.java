package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.repository.MetaDataRepository;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

import static com.salescode.jooq.generated.tables.CkMetadata.CK_METADATA;

@Repository
public class MetaDataRepositoryImpl implements MetaDataRepository {

    private final DSLContext dsl;
    @PersistenceContext
    EntityManager entityManager;

    public MetaDataRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<CkMetadata> findByDomainNameAndDomainType(String domainName, String domainType) {
        return dsl.selectFrom(CK_METADATA) // Replace CK_METADATA with your actual jOOQ table
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .fetchOptionalInto(CkMetadata.class); // Map result to CkMetadata POJO
    }

    public List<CkMetadata> findAll(String domainName) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .fetchInto(CkMetadata.class);
    }

    public CkMetadata findByValue(String domainName, String domainType) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .fetchOneInto(CkMetadata.class);
    }

    public CkMetadata merge(CkMetadata metaData) {
        return entityManager.merge(metaData);
    }

    public List<CkMetadata> findByDomainType(String domainType) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .fetchInto(CkMetadata.class);
    }

    public List<CkMetadata> findByDomainName(String domainName) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .fetchInto(CkMetadata.class);
    }

    public CkMetadata deleteByValue(String domainName, String domainType) {
        CkMetadata record = dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .fetchOneInto(CkMetadata.class);
        return record;
    }
}
