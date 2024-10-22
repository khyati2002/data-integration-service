package com.salescode.channelkart.validations;

import com.salescode.channelkart.exceptions.IllegalArgumentException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.security.Function;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class ValidationService {

    public static final String PREFIX = "batch-{}";
    private static final String VALIDATION_GROUP_KEY = "ck_validation_rule";
//    @Autowired
//    GroupInfoController groupInfoController;

    public ValidationResult validate(CommonDataModel cdm, Optional<String> validationExcludeGroupName) {

        String lob = SecurityContextUtils.getLob();
        List<String> validationExcludeGroup = validationExcludeGroupName.isPresent() && StringUtils.isNotBlank(validationExcludeGroupName.get()) ? getExcludeObjectIds(VALIDATION_GROUP_KEY, validationExcludeGroupName.get()) : new ArrayList<>();

        List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob, cdm.getClass().getSimpleName());

        List<RuleResult> ruleResult = rules != null ? rules.stream().filter(rule -> !(validationExcludeGroup.contains(rule.getId()))).map(f -> RuleLogicEngine.INSTANCE.execute(cdm, f)).collect(Collectors.toList()) : new ArrayList<>();

        return evaluateResults(ruleResult);

    }

    public ValidationResult batchValidate(CommonDataModel cdm, Optional<String> validationExcludeGroupName) {

        String lob = SecurityContextUtils.getLob();
        List<String> validationExcludeGroup = validationExcludeGroupName.isPresent() && StringUtils.isNotBlank(validationExcludeGroupName.get()) ? getExcludeObjectIds(VALIDATION_GROUP_KEY, validationExcludeGroupName.get()) : new ArrayList<>();


        List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob, StringUtils.format(PREFIX, cdm.getClass().getSimpleName()));

        List<RuleResult> ruleResult = rules != null ? rules.stream().filter(rule -> !(validationExcludeGroup.contains(rule.getId()))).map(f -> RuleLogicEngine.INSTANCE.execute(cdm, f)).collect(Collectors.toList()) : new ArrayList<>();
        return evaluateResults(ruleResult);

    }

    private ValidationResult evaluateResults(List<RuleResult> ruleResult) {

        Map<Status, List<RuleResult>> resultsByStatus = ruleResult.stream().collect(Collectors.groupingBy(RuleResult::getStatus));

        Function<Status> errorStatus = () -> resultsByStatus.get(Status.CONFLICT) != null ? Status.CONFLICT : Status.ERROR;

        Status status = (resultsByStatus.get(Status.ERROR) == null && resultsByStatus.get(Status.CONFLICT) == null) ? Status.OK : errorStatus.invoke();

        ValidationResult vr = new ValidationResult(status);

        if (resultsByStatus.get(Status.ERROR) != null) vr.getViolations().addAll(resultsByStatus.get(Status.ERROR));

        if (resultsByStatus.get(Status.WARNING) != null) vr.getViolations().addAll(resultsByStatus.get(Status.WARNING));

        if (resultsByStatus.get(Status.CONFLICT) != null)
            vr.getViolations().addAll(resultsByStatus.get(Status.CONFLICT));

        if (resultsByStatus.get(Status.OK) != null) {
            vr.getSuccessMessages().addAll(resultsByStatus.get(Status.OK));
        }
        return vr;
    }

    public ValidationResult validate(CommonDataModel cdm, String type) {
        if (cdm == null) {
            throw new IllegalArgumentException("Cannot validate null object passed as argument");
        }
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Invalid type passed as null/blank for {}", cdm.toString());
        }
        String lob = SecurityContextUtils.getLob();
        List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob, type);
        List<RuleResult> ruleResult = rules != null ? rules.stream().map(f -> RuleLogicEngine.INSTANCE.execute(cdm, f)).collect(Collectors.toList()) : new ArrayList<>();
        return evaluateResults(ruleResult);
    }

    private List<String> getExcludeObjectIds(String type, String name) {
//        List<String> objectIds = AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob() + ":" + type, name, k -> groupInfoController.readGroup(type, name).getObjectIdList());
//        return objectIds.isEmpty() ? new ArrayList<>(1) : objectIds;
        return new ArrayList<>(1);
    }

}
