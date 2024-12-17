package com.salescode.dataintegration.etl.transformer.registry;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.repository.TransformerInfoRepository;
import com.salescode.channelkart.transformers.TransformerInfo;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransformerInfoRegistry extends RefreshableRegistry {

    private final Map<String, TransformerInfo> transformerCache = new ConcurrentHashMap<>();
    private final Map<String, String> nameToIdCache = new ConcurrentHashMap<>();
    private final TransformerInfoRepository transformerInfoRepository;

    @Autowired
    public TransformerInfoRegistry(TransformerInfoRepository transformerInfoRepository) {
        this.transformerInfoRepository = transformerInfoRepository;
    }

    /**
     * Retrieve an active TransformerInfo by ID, loading from the database if not cached.
     *
     * @param id the ID of the transformer info
     * @return TransformerInfo if found and active
     * @throws IllegalArgumentException if the transformer is not found or inactive
     */
    public TransformerInfo getTransformerInfoById(String id) {
        // Check cache by ID, and load from DB if absent
        return transformerCache.computeIfAbsent(id, this::loadTransformerInfoById);
    }

    /**
     * Retrieve an active TransformerInfo by name, first getting ID from nameToIdCache, then fetching the actual transformer info.
     *
     * @param name the name of the transformer info
     * @return TransformerInfo if found and active
     * @throws IllegalArgumentException if the transformer is not found or inactive
     */
    public TransformerInfo getTransformerInfoByName(String name) {
        // Attempt to get the ID corresponding to the name
        String id = nameToIdCache.computeIfAbsent(name, this::loadIdByName);
        if (id == null) {
            throw new IllegalArgumentException("Transformer with name " + name + " not found or inactive");
        }
        // Use the ID to get the transformer info (this will cache the result if not present)
        return getTransformerInfoById(id);
    }

    /**
     * Load a transformer by ID from the database if active.
     *
     * @param id the transformer ID
     * @return the loaded TransformerInfo or null if not found or inactive
     */
    private TransformerInfo loadTransformerInfoById(String id) {
        TransformerInfo info = transformerInfoRepository.findByIdAndActiveStatus(id, ActiveStatus.ACTIVE);
        if (info != null && info.getName() != null) {
            nameToIdCache.put(info.getName(), info.getId()); // Update name-to-ID cache
        }
        return info;
    }

    /**
     * Load the ID of an active transformer by name from the database.
     *
     * @param name the name of the transformer info
     * @return the ID if found and active, otherwise null
     */
    private String loadIdByName(String name) {
        return transformerInfoRepository.findIdByName(name);
//        return dsl.select(CK_TRANSFORMER_INFO.ID)
//                .from(CK_TRANSFORMER_INFO)
//                .where(CK_TRANSFORMER_INFO.NAME.eq(name))
//                .and(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
//                .fetchOne(CK_TRANSFORMER_INFO.ID);
    }

    public void init() {
        List<TransformerInfo> info = transformerInfoRepository.findAllByActiveStatus(ActiveStatus.ACTIVE);
        info.stream().filter(transformerInfo -> NullUtils.isNotNull(transformerInfo.getName()) && NullUtils.isNotNull(transformerInfo.getId())).parallel().forEach(transformerInfo -> {
            transformerCache.put(transformerInfo.getId(), transformerInfo);
            nameToIdCache.put(transformerInfo.getName(), transformerInfo.getId());
        });
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        transformerCache.clear();
        nameToIdCache.clear();
    }
}