package com.salescode.dataintegration.etl.metadata.registry;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.jooq.generated.Tables.CK_METADATA;

@Service
public class MetadataRegistry implements RefreshableRegistry {

    private final DSLContext dsl;
    private final Map<String, CkMetadata> metadataCache = new ConcurrentHashMap<>();

    @Autowired
    public MetadataRegistry(DSLContext dsl) {
        this.dsl = dsl;
    }

    public CkMetadata getMetadataById(String id) {
        // Check cache first
        return metadataCache.computeIfAbsent(id, this::loadMetadataById);
    }

    public CkMetadata getMetadataByDomainNameAndType(String domainName, String domainType) {
        // Use a unique key for domainName and domainType
        String key = domainName + ":" + domainType;
        return metadataCache.computeIfAbsent(key, k -> loadMetadataByDomainAndType(domainName, domainType));
    }

    private CkMetadata loadMetadataById(String id) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.ID.eq(id))
                .and(CK_METADATA.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOneInto(CkMetadata.class);
    }

    private CkMetadata loadMetadataByDomainAndType(String domainName, String domainType) {
        return dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .and(CK_METADATA.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOneInto(CkMetadata.class);
    }

    @Override
    public void refreshRegistry() {
        metadataCache.clear();
    }
}