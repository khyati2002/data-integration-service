package com.salescode.dim.transformers.registry;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.interfaces.RefreshableRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import org.jooq.DSLContext;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.dim.jooq.generated.Tables.CK_TRANSFORMER_INFO;

/**
 * A singleton registry for transformer information.
 * <p>
 * This class is serializable. The DSLContext is transient and must be reinitialized
 * after deserialization if needed.
 * <p>
 */
public class TransformerInfoRegistry implements RefreshableRegistry, Serializable {

    private static final long serialVersionUID = -2593827028194431363L;

    private static TransformerInfoRegistry instance;

    private final Map<String, TransformerInfo> transformerCache = new ConcurrentHashMap<>();
    private final Map<String, String> nameToIdCache = new ConcurrentHashMap<>();

    private transient DSLContext dsl;

    private TransformerInfoRegistry(DSLContext dsl) {
        // Defensive check for null DSLContext.
        this.dsl = Objects.requireNonNull(dsl, "DSLContext cannot be null");
    }

    /**
     * Returns the singleton instance, initializing it if necessary and calling init() to preload data.
     *
     * @param dsl the DSLContext to use for database operations.
     * @return the singleton instance.
     */
    public static synchronized TransformerInfoRegistry getInstance(DSLContext dsl) {
        if (dsl == null) {
            throw new IllegalArgumentException("DSLContext cannot be null");
        }
        if (instance == null) {
            instance = new TransformerInfoRegistry(dsl);
            instance.init(); // Preload transformer info
        }
        return instance;
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance.
     * @throws IllegalStateException if the instance has not been initialized.
     */
    public static TransformerInfoRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TransformerInfoRegistry has not been initialized. " +
                    "Call getInstance(DSLContext) first.");
        }
        return instance;
    }

    /**
     * Retrieve an active TransformerInfo by ID, loading it from the database if not cached.
     *
     * @param id the transformer info ID.
     * @return the corresponding TransformerInfo.
     * @throws IllegalArgumentException if the id is null or no active transformer is found.
     */
    public TransformerInfo getTransformerInfoById(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Transformer ID cannot be null");
        }
        return transformerCache.computeIfAbsent(id, this::loadTransformerInfoById);
    }

    /**
     * Retrieve an active TransformerInfo by name.
     *
     * @param name the name of the transformer.
     * @return the corresponding TransformerInfo.
     * @throws IllegalArgumentException if the name is null or no active transformer is found.
     */
    public TransformerInfo getTransformerInfoByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Transformer name cannot be null");
        }
        String id = nameToIdCache.computeIfAbsent(name, this::loadIdByName);
        if (id == null) {
            throw new IllegalArgumentException("Transformer with name " + name + " not found or inactive");
        }
        return getTransformerInfoById(id);
    }

    /**
     * Loads a transformer info from the database by its ID if it is active.
     *
     * @param id the transformer ID.
     * @return the loaded TransformerInfo, or null if not found or inactive.
     */
    private TransformerInfo loadTransformerInfoById(String id) {
        TransformerInfo record = dsl.selectFrom(CK_TRANSFORMER_INFO)
                                      .where(CK_TRANSFORMER_INFO.ID.eq(id))
                                      .and(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                                      .fetchOneInto(TransformerInfo.class);
        if (record != null && record.getName() != null) {
            nameToIdCache.put(record.getName(), record.getId());
        }
        return record;
    }

    /**
     * Loads the ID of an active transformer from the database by its name.
     *
     * @param name the transformer name.
     * @return the transformer ID if found, otherwise null.
     */
    private String loadIdByName(String name) {
        return dsl.select(CK_TRANSFORMER_INFO.ID)
                  .from(CK_TRANSFORMER_INFO)
                  .where(CK_TRANSFORMER_INFO.NAME.eq(name))
                  .and(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                  .fetchOne(CK_TRANSFORMER_INFO.ID);
    }

    /**
     * Initializes the registry by preloading all active transformer info from the database.
     * <p>
     * This method uses a bulk fetch and parallel processing for optimal performance.
     * </p>
     */
    public void init() {
        List<TransformerInfo> infoList = dsl.selectFrom(CK_TRANSFORMER_INFO)
                                              .where(CK_TRANSFORMER_INFO.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                                              .and(CK_TRANSFORMER_INFO.ID.isNotNull())
                                              .and(CK_TRANSFORMER_INFO.NAME.isNotNull())
                                              .fetchInto(TransformerInfo.class);
        infoList.parallelStream()
                .filter(record -> record.getName() != null && record.getId() != null)
                .forEach(record -> {
                    transformerCache.put(record.getId(), record);
                    nameToIdCache.put(record.getName(), record.getId());
                });
    }

    /**
     * Clears the registry caches, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        transformerCache.clear();
        nameToIdCache.clear();
    }

    /**
     * Ensures that the singleton property is maintained during deserialization.
     *
     * @return the singleton instance.
     * @throws ObjectStreamException if an error occurs during deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        return getInstance();
    }

    /**
     * Sets the DSLContext. Use this method to reinitialize the transient DSLContext after deserialization.
     *
     * @param dsl the DSLContext to set.
     */
    public void setDslContext(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "DSLContext cannot be null");
    }
}