package com.salescode.dataintegration.etl.validation.registry;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.repository.RuleRepository;
import com.salescode.channelkart.validations.RuleInfo;
import com.salescode.dataintegration.etl.interfaces.RefreshableRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class ValidationInfoRegistry extends RefreshableRegistry {

    private final Map<String, List<RuleInfo>> validationCache = new ConcurrentHashMap<>();
    private final RuleRepository ruleRepository;

    @Autowired
    public ValidationInfoRegistry(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * Retrieve a list of active RuleInfo by type, loading from the database if not cached.
     *
     * @param type the type of validation rule
     * @return list of RuleInfo if found and active
     * @throws IllegalArgumentException if no validation rule is found for the type
     */
    public List<RuleInfo> getValidationRulesByType(String type) {
        // Check cache by type, and load from DB if absent
        return validationCache.computeIfAbsent(type, this::loadValidationsByType);
    }

    /**
     * Load a list of validations by type from the database if active.
     *
     * @param type the type of validation rule
     * @return list of RuleInfo or an empty list if none are found or active
     */
    private List<RuleInfo> loadValidationsByType(String type) {
        return ruleRepository.findAllByTypeAndActiveStatus(type, ActiveStatus.ACTIVE);
//        return dsl.selectFrom(CK_VALIDATION_RULE)
//                .where(CK_VALIDATION_RULE.TYPE.eq(type))
//                .and(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
//                .fetchInto(RuleInfo.class);
    }

    public void init() {
        Map<String, List<RuleInfo>> collect = ruleRepository.findAllByActiveStatus(ActiveStatus.ACTIVE).stream().collect(Collectors.groupingBy(RuleInfo::getType, HashMap::new, toList()));
//        Stream<RuleInfo> ckValidationRuleStream = dsl.selectFrom(CK_VALIDATION_RULE)
//                .where(CK_VALIDATION_RULE.ACTIVE_STATUS.eq(ActiveStatus.ACTIVE))
//                .groupBy(CK_VALIDATION_RULE.TYPE)
//                .fetchStreamInto(RuleInfo.class);
//        ConcurrentMap<String, List<RuleInfo>> collect = ckValidationRuleStream.collect(Collectors.groupingByConcurrent(RuleInfo::getType));
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