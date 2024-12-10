package com.salescode.dataintegration.etl.validation.registry;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import com.salescode.jooq.generated.tables.pojos.CkValidationRule;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.salescode.jooq.generated.Tables.CK_VALIDATION_RULE;

@Service
public class ValidationInfoRegistry implements RefreshableRegistry {

    private final DSLContext dsl;
    private final Map<String, List<CkValidationRule>> validationCache = new ConcurrentHashMap<>();

    @Autowired
    public ValidationInfoRegistry(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Retrieve a list of active CkValidationRule by type, loading from the database if not cached.
     *
     * @param type the type of validation rule
     * @return list of CkValidationRule if found and active
     * @throws IllegalArgumentException if no validation rule is found for the type
     */
    public List<CkValidationRule> getValidationRulesByType(String type) {
        // Check cache by type, and load from DB if absent
        return validationCache.computeIfAbsent(type, this::loadValidationsByType);
    }

    /**
     * Load a list of validations by type from the database if active.
     *
     * @param type the type of validation rule
     * @return list of CkValidationRule or an empty list if none are found or active
     */
    private List<CkValidationRule> loadValidationsByType(String type) {
        return dsl.selectFrom(CK_VALIDATION_RULE)
                .where(CK_VALIDATION_RULE.TYPE.eq(type))
                .and(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchInto(CkValidationRule.class);
    }

    public void init() {
        Stream<CkValidationRule> ckValidationRuleStream = dsl.selectFrom(CK_VALIDATION_RULE)
                .where(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .groupBy(CK_VALIDATION_RULE.TYPE)
                .fetchStreamInto(CkValidationRule.class);
        ConcurrentMap<String, List<CkValidationRule>> collect = ckValidationRuleStream.collect(Collectors.groupingByConcurrent(CkValidationRule::getType));
        validationCache.putAll(collect);
    }

    /**
     * Clear the cache, forcing fresh database loads for future requests.
     */
    @Override
    public void refreshRegistry() {
        validationCache.clear();
    }
}