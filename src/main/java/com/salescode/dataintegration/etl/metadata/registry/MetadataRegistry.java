package com.salescode.dataintegration.etl.metadata.registry;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.repository.MetaDataRepository;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.channelkart.models.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetadataRegistry extends RefreshableRegistry {

    private final Map<String, MetaData> metadataCache = new ConcurrentHashMap<>();
    private final Map<String, String> nameToIdCache = new ConcurrentHashMap<>();
    private final MetaDataRepository metadataRepository;

    @Autowired
    public MetadataRegistry(MetaDataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    /**
     * Retrieve an active MetaData by ID, loading from the database if not cached.
     *
     * @param id the ID of the metadata
     * @return an Optional containing MetaData if found and active, or empty otherwise
     */
    public Optional<MetaData> getMetadataById(String id) {
        return Optional.ofNullable(metadataCache.computeIfAbsent(id, this::loadMetadataById));
    }

    /**
     * Retrieve an active MetaData by domain name and type, checking cache by ID to prevent duplicate entries.
     *
     * @param domainName The domain name of the metadata
     * @param domainType The domain type of the metadata
     * @return an Optional containing MetaData if found and active, or empty otherwise
     */
    public Optional<MetaData> getMetadataByDomainNameAndType(String domainName, String domainType) {
        String key = domainName + ":" + domainType;
        Optional<String> idOpt = Optional.ofNullable(nameToIdCache.computeIfAbsent(key, k -> loadIdByDomainAndType(domainName, domainType)));
        return idOpt.flatMap(this::getMetadataById);
    }

    /**
     * Load metadata by ID from the database if active, and update the name-to-ID cache.
     *
     * @param id the metadata ID
     * @return the loaded MetaData or null if not found or inactive
     */
    private MetaData loadMetadataById(String id) {
        MetaData metadata = metadataRepository.findByIdAndActiveStatus(id,ActiveStatus.ACTIVE);
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
        return metadataRepository.getIdByDomainNameAndDomainType(domainName, domainType);
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