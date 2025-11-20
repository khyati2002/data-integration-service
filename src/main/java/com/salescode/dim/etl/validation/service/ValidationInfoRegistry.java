package com.salescode.dim.etl.validation.service;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.interfaces.RefreshableRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.ValidationRule;
import org.jooq.DSLContext;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_VALIDATION_RULE;

public class ValidationInfoRegistry implements RefreshableRegistry, Serializable {

    private static final long serialVersionUID = 9061028959661625271L;

    private DSLContext dsl;
    private final Map<String, List<ValidationRule>> validationCache = new ConcurrentHashMap<>();

    /**
     * Constructs a ValidationInfoRegistry and immediately preloads validation rules.
     *
     * @param dsl the DSLContext for database operations
     */
    public ValidationInfoRegistry(DSLContext dsl) {
        this.dsl = dsl;
        init(); // Preload validation rules on construction
    }

    /**
     * Retrieve a list of active ValidationRule by type from the cache.
     *
     * @param type the type of validation rule
     * @return list of ValidationRule if found and active, empty list otherwise
     */
    public List<ValidationRule> getValidationRulesByType(String type) {
        return validationCache.computeIfAbsent(type, this::loadValidationsByType);
    }

    /**
     * Load a list of validations by type from the database if active.
     *
     * @param type the type of validation rule
     * @return list of CkValidationRule or an empty list if none are found or active
     */
    private List<ValidationRule> loadValidationsByType(String type) {
        return dsl.selectFrom(CK_VALIDATION_RULE)
                  .where(CK_VALIDATION_RULE.TYPE.eq(type))
                  .and(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                  .orderBy(CK_VALIDATION_RULE.PRIORITY.asc())
                  .fetchInto(ValidationRule.class);
    }

    /**
     * Initializes the registry by preloading all active validation rules into the cache.
     */
    public void init() {
        // Load all active validation rules and group by type
        Map<String, List<ValidationRule>> rules = dsl.selectFrom(CK_VALIDATION_RULE)
                .where(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
                .fetchInto(ValidationRule.class)
                .stream()
                .collect(Collectors.groupingBy(rule -> {
                    String type = rule.getType();
                    // Remove "ev-" prefix if present
                    return type.startsWith("ev-") ? type.substring(3) : type;
                }));


        // Populate the cache
        validationCache.putAll(rules);
    }

    /**
     * Refreshes the registry by reloading all validation rules from the database.
     */
    @Override
    public void refreshRegistry() {
        validationCache.clear();
        init();
    }

    /**
     * Sets the DSLContext. Use this method to reinitialize the transient DSLContext after deserialization.
     *
     * @param dsl the DSLContext to set.
     * @throws NullPointerException if dsl is null
     */
    public void setDslContext(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "DSLContext cannot be null");
    }
}