package com.salescode.dim.etl.validation.service;

import com.salescode.dim.DatabaseConnectionUtil;
import com.salescode.dim.PropertyLoader;
import com.salescode.dim.interfaces.RefreshableRegistry;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;

public class ValidationExcludeGroupRegistry implements RefreshableRegistry, Serializable {

    private static final long serialVersionUID = 9061028959661625271L;
    private final Map<String, Set<String>> objectIdListCache = new ConcurrentHashMap<>();
    private final transient DSLContext dsl;

    /**
     * Constructs a ValidationInfoRegistry and immediately preloads validation rules.
     *
     * @param dsl the DSLContext for database operations
     */
    public ValidationExcludeGroupRegistry(DSLContext dsl) {
        this.dsl = dsl;
        init(); // Preload validation rules on construction
    }

    /**
     * Main method for testing the ValidationExcludeGroupRegistry
     */
    public static void main(String[] args) {
        try {
            // Load database properties
            final Map<String, Properties> applicationProperties = PropertyLoader.loadApplicationProperties(null);

            Properties commonProperties = applicationProperties.getOrDefault("Common", new Properties());

            var connection = DatabaseConnectionUtil.createConnection(commonProperties);
            var dslContext = DatabaseConnectionUtil.createDSLContext(connection);

            // Initialize and return the registry
            System.out.println("Initializing ValidationExcludeGroupRegistry...");
            ValidationExcludeGroupRegistry registry = new ValidationExcludeGroupRegistry(dslContext);

            // Print all cached keys and their associated object ID lists
            System.out.println("Cached entity groups:");
            for (Map.Entry<String, Set<String>> entry : registry.objectIdListCache.entrySet()) {
                System.out.println("Group Key: " + entry.getKey());
                System.out.println("Object IDs: " + String.join(", ", entry.getValue()));
                System.out.println("-----------------------------------");
            }

            // Test retrieval method
            String testKey = "outlet_validation_exclude";
            System.out.println("\nTesting retrieval for key: " + testKey);
            Set<String> objectIds = registry.getObjectIdListByKey(testKey);

            if (objectIds != null) {
                System.out.println("Found " + objectIds.size() + " objects for key: " + testKey);
                for (String id : objectIds) {
                    System.out.println(" - " + id);
                }
            } else {
                System.out.println("No objects found for key: " + testKey);
            }

            // Test refreshing registry
            System.out.println("\nRefreshing registry...");
            registry.refreshRegistry();
            System.out.println("Registry refreshed. Cache size: " + registry.objectIdListCache.size());

        } catch (Exception e) {
            System.err.println("Error during registry testing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        // Query entity groups from database
        Result<Record3<String, String, JsonNode>> result = dsl.select(
                CK_GENERIC_OBJECT.KEY1,
                CK_GENERIC_OBJECT.KEY2,
                CK_GENERIC_OBJECT.PAYLOAD)
                .from(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq("entity-group"))
                .fetch();

        // Process results and populate cache
        result.forEach(this::processAndCacheRecord);
    }

    /**
     * Processes a database record and adds it to the cache if valid
     *
     * @param record The database record containing entity group data
     */
    private void processAndCacheRecord(Record3<String, String, JsonNode> record) {
        String key = record.get(CK_GENERIC_OBJECT.KEY1);
        JsonNode payload = record.get(CK_GENERIC_OBJECT.PAYLOAD);

        if (key == null || payload == null || !payload.has("objectIdList")) {
            return;
        }

        Set<String> idList = extractObjectIds(payload.get("objectIdList"));
        if (!idList.isEmpty()) {
            objectIdListCache.put(key, idList);
        }
    }

    /**
     * Extracts object IDs from a JSON node
     *
     * @param objectIdListNode JSON node containing object IDs
     * @return Set of extracted object IDs
     */
    private Set<String> extractObjectIds(JsonNode objectIdListNode) {
        Set<String> idList = new HashSet<>();

        if (objectIdListNode != null && objectIdListNode.isArray()) {
            objectIdListNode.forEach(idNode -> {
                if (idNode.isTextual()) {
                    idList.add(idNode.asText());
                }
            });
        }

        return idList;
    }

    /**
     * Retrieves the object ID list for a given key.
     *
     * @param key the key1 value to look up
     * @return the Set of object IDs associated with the key, or null if not found
     */
    public Set<String> getObjectIdListByKey(String key) {
        return objectIdListCache.get(key);
    }

    @Override
    public void refreshRegistry() {
        objectIdListCache.clear();
        init();
    }

}
