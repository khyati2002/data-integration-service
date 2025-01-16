package com.applicate.services.channelkart.validations;

import com.applicate.services.channelkart.exceptions.IllegalArgumentException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.security.Function;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class ValidationService {

//	@Autowired
//	GroupInfoController groupInfoController;

	private static final String VALIDATION_GROUP_KEY="ck_validation_rule";

	public static final String PREFIX = "batch-{}";
	private final DistributedCache distributedCache;
	private final GenericEntityRepository genericEntityRepository;

	public ValidationService(DistributedCache distributedCache, GenericEntityRepository genericEntityRepository) {
		this.distributedCache = distributedCache;
		this.genericEntityRepository = genericEntityRepository;
	}

	public ValidationResult validate(CommonDataModel cdm,Optional<String> validationExcludeGroupName) {

		String lob = SecurityContextUtils.getLob();
		List<String> validationExcludeGroup=validationExcludeGroupName.isPresent() && StringUtils.isNotBlank(validationExcludeGroupName.get()) ?getExcludeObjectIds(VALIDATION_GROUP_KEY,validationExcludeGroupName.get()):new ArrayList<>();

		List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob,cdm.getClass().getSimpleName());

		List<RuleResult> ruleResult = rules!=null? rules.stream().filter(rule->!(validationExcludeGroup.contains(rule.getId())))
				.map(f->RuleLogicEngine.INSTANCE.execute(cdm, f))
				.collect(Collectors.toList()):new ArrayList<>();

				return evaluateResults(ruleResult);

	}
	
    public ValidationResult batchValidate(CommonDataModel cdm, Optional<String> validationExcludeGroupName) {
		
		String lob = SecurityContextUtils.getLob();
		List<String> validationExcludeGroup=validationExcludeGroupName.isPresent() && StringUtils.isNotBlank(validationExcludeGroupName.get()) ?getExcludeObjectIds(VALIDATION_GROUP_KEY,validationExcludeGroupName.get()):new ArrayList<>();


		List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob,StringUtils.format(PREFIX, cdm.getClass().getSimpleName()));

		List<RuleResult> ruleResult = rules!=null? rules.stream().filter(rule->!(validationExcludeGroup.contains(rule.getId())))
				.map(f->RuleLogicEngine.INSTANCE.execute(cdm, f))
				.collect(Collectors.toList()):new ArrayList<>();
		return  evaluateResults(ruleResult);

	}

	private ValidationResult evaluateResults(List<RuleResult> ruleResult) {

		Map<Status, List<RuleResult>> resultsByStatus =  ruleResult.stream().collect(Collectors.groupingBy(RuleResult::getStatus));

		Function<Status> errorStatus = ()->resultsByStatus.get(Status.CONFLICT)!=null?Status.CONFLICT:Status.ERROR;

		Status status = (resultsByStatus.get(Status.ERROR)==null && resultsByStatus.get(Status.CONFLICT)==null) ? Status.OK:errorStatus.invoke();

		ValidationResult vr=new ValidationResult(status);

		if(resultsByStatus.get(Status.ERROR)!=null)
			vr.getViolations().addAll(resultsByStatus.get(Status.ERROR));

		if(resultsByStatus.get(Status.WARNING)!=null)
			vr.getViolations().addAll(resultsByStatus.get(Status.WARNING));

		if(resultsByStatus.get(Status.CONFLICT)!=null)
			vr.getViolations().addAll(resultsByStatus.get(Status.CONFLICT));

		if(resultsByStatus.get(Status.OK)!=null) {
			vr.getSuccessMessages().addAll(resultsByStatus.get(Status.OK));
		}
		return vr;
	}

	public ValidationResult validate(CommonDataModel cdm, String type) {
		if(cdm == null) {
			throw new IllegalArgumentException("Cannot validate null object passed as argument");
		}
		if(StringUtils.isEmpty(type)) {
			throw new IllegalArgumentException("Invalid type passed as null/blank for {}",cdm.toString());
		}
		String lob = SecurityContextUtils.getLob();
		List<RuleInfo> rules = RuleRegistry.INSTANCE.get(lob,type);
		List<RuleResult> ruleResult = rules!=null? rules.stream()
				.map(f->RuleLogicEngine.INSTANCE.execute(cdm, f))
				.collect(Collectors.toList()):new ArrayList<>();
				return evaluateResults(ruleResult);
	}

	private List<String> getExcludeObjectIds(String type,String name){
		return distributedCache.withCache(SecurityContextUtils.getLob(), null, "validationExcludeGroup-"+name,s -> {
			List<GenericEntity> excludeRecord = genericEntityRepository.findByNameAndKey1AndKey2("entity-group", name, type);
			var a = excludeRecord.stream().map(e -> toList((ArrayNode) e.getPayload().get("objectIdList"))).findFirst().orElse(null);
            return ObjectUtils.isNotEmpty(a) ? a : new ArrayList<>();
		});
	}

	private List<String> toList(ArrayNode an){
		List<String> oidList = new ArrayList<>();
		an.forEach(n-> oidList.add(n.textValue()));
		return oidList;
	}
}
