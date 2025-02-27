package com.salescode.dim.etl.validation.service;

import com.salescode.dim.interfaces.RefreshableRegistry;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;

public class ValidationExcludeGroupRegistry implements RefreshableRegistry, Serializable {

    private static final long serialVersionUID = 9061028959661625271L;
    private final Map<String, List<String>> objectIdListCache = new ConcurrentHashMap<>();
    //    private final Map<String, List<ValidationRule>> validationCache = new ConcurrentHashMap<>();
    private transient DSLContext dsl;

    /**
     * Constructs a ValidationInfoRegistry and immediately preloads validation rules.
     *
     * @param dsl the DSLContext for database operations
     */
    public ValidationExcludeGroupRegistry(DSLContext dsl) {
        this.dsl = dsl;
        init(); // Preload validation rules on construction
    }

    @Override
    public void init() {
        Result<Record3<String, String, JsonNode>> result = dsl.select(CK_GENERIC_OBJECT.KEY1, CK_GENERIC_OBJECT.KEY2, CK_GENERIC_OBJECT.PAYLOAD)
                .from(CK_GENERIC_OBJECT)
                .where(CK_GENERIC_OBJECT.NAME.eq("entity-group"))
                .fetch()
                .into(CK_GENERIC_OBJECT.KEY1, CK_GENERIC_OBJECT.KEY2, CK_GENERIC_OBJECT.PAYLOAD);

        for (Record3<String, String, JsonNode> record : result) {
            String key1 = record.get(CK_GENERIC_OBJECT.KEY1);
            JsonNode payload = record.get(CK_GENERIC_OBJECT.PAYLOAD);

            if (payload != null && payload.has("objectIdList")) {
                
                List<String> idList = new ArrayList<>();
                JsonNode objectIdListNode = payload.get("objectIdList");
                if (objectIdListNode.isArray()) {
                    for (JsonNode idNode : objectIdListNode) {
                        if (idNode.isTextual()) {
                            idList.add(idNode.asText());
                        }
                    }
                }
                objectIdListCache.put(key1, idList);
            }
        }
    }


    /**
     * Retrieves the object ID list for a given key.
     *
     * @param key the key1 value to look up
     * @return the list of object IDs associated with the key, or null if not found
     */
    public List<String> getObjectIdListByKey(String key) {
        return objectIdListCache.get(key);
    }

    @Override
    public void refreshRegistry() {
        objectIdListCache.clear();
        init();
    }
}
