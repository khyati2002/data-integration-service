package com.salescode.dataintegration.etl.metadata.registry;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.jooq.generated.Tables.CK_METADATA;

@Service
public class MetadataRegistry implements RefreshableRegistry {

    private final DSLContext dsl;
    private final Map<String, CkMetadata> metadataCache = new ConcurrentHashMap<>();
    private final Map<String, String> nameToIdCache = new ConcurrentHashMap<>();

    @Autowired
    public MetadataRegistry(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Retrieve an active CkMetadata by ID, loading from the database if not cached.
     *
     * @param id the ID of the metadata
     * @return an Optional containing CkMetadata if found and active, or empty otherwise
     */
    public Optional<CkMetadata> getMetadataById(String id) {
        return Optional.ofNullable(metadataCache.computeIfAbsent(id, this::loadMetadataById));
    }

    /**
     * Retrieve an active CkMetadata by domain name and type, checking cache by ID to prevent duplicate entries.
     *
     * @param domainName The domain name of the metadata
     * @param domainType The domain type of the metadata
     * @return an Optional containing CkMetadata if found and active, or empty otherwise
     */
    public Optional<CkMetadata> getMetadataByDomainNameAndType(String domainName, String domainType) {
        String key = domainName + ":" + domainType;
        Optional<String> idOpt = Optional.ofNullable(nameToIdCache.computeIfAbsent(key, k -> loadIdByDomainAndType(domainName, domainType)));
        return idOpt.flatMap(this::getMetadataById);
    }

    /**
     * Load metadata by ID from the database if active, and update the name-to-ID cache.
     *
     * @param id the metadata ID
     * @return the loaded CkMetadata or null if not found or inactive
     */
    private CkMetadata loadMetadataById(String id) {
        CkMetadata metadata = dsl.selectFrom(CK_METADATA)
                .where(CK_METADATA.ID.eq(id))
                .and(CK_METADATA.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOneInto(CkMetadata.class);
        if (metadata != null) {
            String key = metadata.getDomainName() + ":" + metadata.getDomainType();
            nameToIdCache.put(key, metadata.getId()); // Update name-to-ID cache
        }
        return metadata;
    }

    /**
     * Load the ID of an active metadata by domain name and type from the database.
     *
     * @param domainName The domain name of the metadata
     * @param domainType The domain type of the metadata
     * @return the ID if found and active, otherwise null
     */
    private String loadIdByDomainAndType(String domainName, String domainType) {
        return dsl.select(CK_METADATA.ID)
                .from(CK_METADATA)
                .where(CK_METADATA.DOMAIN_NAME.eq(domainName))
                .and(CK_METADATA.DOMAIN_TYPE.eq(domainType))
                .and(CK_METADATA.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOne(CK_METADATA.ID);
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        metadataCache.clear();
        nameToIdCache.clear();
    }
}