package com.salescode.dataintegration.etl.transformer.registry;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.jooq.generated.Tables.CK_TRANSFORMER_INFO;

@Service
public class TransformerInfoRegistry implements RefreshableRegistry {

    private final DSLContext dsl;
    private final Map<String, CkTransformerInfo> transformerCache = new ConcurrentHashMap<>();
    private final Map<String, String> nameToIdCache = new ConcurrentHashMap<>();

    @Autowired
    public TransformerInfoRegistry(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Retrieve an active CkTransformerInfo by ID, loading from the database if not cached.
     *
     * @param id the ID of the transformer info
     * @return CkTransformerInfo if found and active
     * @throws IllegalArgumentException if the transformer is not found or inactive
     */
    public CkTransformerInfo getTransformerInfoById(String id) {
        // Check cache by ID, and load from DB if absent
        return transformerCache.computeIfAbsent(id, this::loadTransformerInfoById);
    }

    /**
     * Retrieve an active CkTransformerInfo by name, first getting ID from nameToIdCache, then fetching the actual transformer info.
     *
     * @param name the name of the transformer info
     * @return CkTransformerInfo if found and active
     * @throws IllegalArgumentException if the transformer is not found or inactive
     */
    public CkTransformerInfo getTransformerInfoByName(String name) {
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
     * @return the loaded CkTransformerInfo or null if not found or inactive
     */
    private CkTransformerInfo loadTransformerInfoById(String id) {
        CkTransformerInfo info = dsl.selectFrom(CK_TRANSFORMER_INFO)
                .where(CK_TRANSFORMER_INFO.ID.eq(id))
                .and(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOneInto(CkTransformerInfo.class);
        if (info != null) {
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
        return dsl.select(CK_TRANSFORMER_INFO.ID)
                .from(CK_TRANSFORMER_INFO)
                .where(CK_TRANSFORMER_INFO.NAME.eq(name))
                .and(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchOne(CK_TRANSFORMER_INFO.ID);
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