/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.dataintegration.etl.cdm.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.component.model.SequenceGenerator;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.SequenceInfoRepository;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.generated.tables.pojos.CkSequenceInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.salescode.channelkart.utils.JSONUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The class SequenceInfoService
 *
 * A generic approach to maintain sequence information in a single entity so every entity doesn't have to maintain its own
 * generator.
 *
 * Use to get updated sequence. As this method runs in Transaction Mandatory mode so make sure
 * caller should also be running in Transactional mode, this helps this service to rollback any unwanted save & to maintain its sequence
 * in case of any exception.
 *
 * To enable this service we need first required to register in metadata. Please follow below example or {@link SequenceInfo}
// *
// * {@code
// *   Example:
// *   domainName : sequence
// *   domainType : generator
// *   domainValues : [{"entity": "com.applicate.services.channelkart.models.OutletDetails", "pattern": "TEST-%s", "fieldName": "outletCode"}]
// * }
// *
// * @see com.applicate.services.channelkart.enrichments.repository.TestOutletDetailsSequenceEnrichment
// *
// * @author  Manish Srivastava
// * @since   Feb 2021
// */
@Service
public class SequenceInfoService extends AbstractCDMService<CkSequenceInfo> {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(SequenceInfoService.class);

    /** The Constant DOMAIN_NAME. */
    private static final String DOMAIN_NAME = "sequence";

    /** The Constant DOMAIN_TYPE. */
    private static final String DOMAIN_TYPE = "generator";

    /** The Constant CONFIGURATION_KEY. */
    private static final String CONFIGURATION_KEY = "configurations";

	/** The metadata service. */
	private final MetaDataService metadataService;

	/** The Constant DEFAULT_TYPE. */
	private static final String TYPE= "none";

	private static final String METADATA_DOMAIN_NAME= "sequenceGenerator";

	private static final String SEQUENCE_LENGTH = "sequenceLength";

	private final SequenceInfoRepository sequenceInfoRepository;

	private static final int DEFAULT_VALUE_FOR_SEQUENCE_LENGTH = 7;

	private static final int DEFAULT_VALUE_FOR_SEQUENCE_START_VALUE = 1;


	/**
	 * Instantiates a new sequence info service.
	 *
	 * @param sequenceInfoRepository the repository
	 * @param metadataService the metadata service
	 */
	public SequenceInfoService(MetaDataService metadataService,SequenceInfoRepository sequenceInfoRepository) {
		this.sequenceInfoRepository = sequenceInfoRepository;
		this.metadataService= metadataService;
	}

//	/**
//	 * Gets the configurations.
//	 *
//	 * @return the configurations
//	 */
//	@SuppressWarnings("unchecked")
//	public Optional<List<SequenceInfo>> getConfigurations(){
//		Object result= AppCacheManager.getInstance().withCache(DOMAIN_NAME, CONFIGURATION_KEY, tmp->{
//			MetaData metadata= metadataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE);
//
//			logger.info("getConfigurations method no param, metadata {}",metadata);
//
//			if(ObjectUtils.isNotEmpty(metadata)) {
//				ArrayNode arrayNode= metadata.getDomainValues();
//				if(ObjectUtils.isNotEmpty(arrayNode) && !arrayNode.isEmpty()) {
//					try {
//						return JSONUtils.getObjectMapper().readValue(arrayNode.toString(), new TypeReference<List<SequenceInfo>>(){});
//						} catch (JsonProcessingException e) {
//						logger.error("stacktrace", e);
//					}
//				}
//			}
//			return null;
//		});
//		return (result == null) ? Optional.empty() : Optional.of((List<SequenceInfo>)result);
//	}
//
//	/**
//	 * Gets the configurations from metadata.
//	 *
//	 * @return the configurations
//	 */
	public JsonNode getMetaConfigurations(String domainName, String domainType){
		CkMetadata sequenceGenerator = metadataService.fetchByValue(domainName, domainType);
		if(ObjectUtils.isEmpty(sequenceGenerator)){
			if(logger.isDebugEnabled()) {
				logger.info("sequenceGenerator configuration not found for domainName: {}, domainType: {}", domainName, domainType);
			}
			return null;
		}
		if(sequenceGenerator.getDomainValues().isEmpty() || !sequenceGenerator.getDomainValues().get(0).has("fields")){
			logger.info("Configuration not found for sequenceGenerator");
			return null;
		}
		JsonNode fields = JSONUtils.toJsonNode(sequenceGenerator.getDomainValues().get(0).get("fields"));
		if(JSONUtils.isNull(fields)){
			logger.info("Key : \"field\" not found in configuration");
			return null;
		}
		return fields;
	}
//
//	/**
//	 * Gets the configuration.
//	 *
//	 * @return the configuration
//	 */
//	@SuppressWarnings("unchecked")
//	public Optional<List<SequenceInfo>> getConfiguration(String entityName){
//		Assert.notNull(entityName, "Illegal class passed in method argument. Found null.");
//		Object result= AppCacheManager.getInstance().withCache(DOMAIN_NAME,entityName,tmp->{
//			Optional<List<SequenceInfo>> data= getConfigurations();
//
//			logger.info("getConfigurations method param1(Class clazz) {}, Optional<List<SequenceInfo>> {}",entityName,data);
//
//			if(data.isPresent()) {
//				List<SequenceInfo> objects= data.get();
//				return objects.stream()
//						.filter(p ->p.isValid() && StringUtils.endsWithIgnoreCase(p.getEntity(), entityName))
//						.collect(Collectors.toList());
//
//			}
//			return null;
//		});
//		return (result == null) ? Optional.empty() : Optional.of((List<SequenceInfo>)result);
//	}
//
//	/**
//	 * Gets the configuration.
//	 *
//	 * @param fieldName the field name
//	 * @return the configuration
//	 */
//	public Optional<List<SequenceInfo>> getConfiguration(String entityName, String fieldName){
//		Assert.notNull(fieldName, "Illegal fieldName passed in method argument. Found null.");
//		Optional<List<SequenceInfo>> configurations= getConfiguration(entityName);
//
//		logger.info("getConfigurations method param1(Class clazz) {} param2(String fieldName) {}, Optional<List<SequenceInfo>> {}",entityName,fieldName,configurations);
//
//		if(configurations.isPresent()) {
//			List<SequenceInfo> result= AppCacheManager.getInstance().withCache(DOMAIN_NAME,String.format("%s:%s",entityName,fieldName),tmp->{
//				List<SequenceInfo> objects= configurations.get();
//				List<SequenceInfo> data= objects.stream()
//						.filter(p -> p.getFieldName().equals(fieldName))
//						.collect(Collectors.toList());
//				return (ObjectUtils.isNotEmpty(data)) ? data : null;
//			});
//			return (result == null) ? Optional.empty() : Optional.of(result);
//		}
//		return Optional.empty();
//	}
//
//	/**
//	 * Gets the configuration.
//	 *
//	 * @param fieldName the field name
//	 * @param type the type
//	 * @return the configuration
//	 */
//	public Optional<SequenceInfo> getConfiguration(String entityName, String fieldName, String type){
//		Assert.notNull(fieldName, "Illegal fieldName passed in method argument. Found null.");
//		Assert.notNull(type, "Illegal type passed in method argument. Found null.");
//		Optional<List<SequenceInfo>> configurations= getConfiguration(entityName,fieldName);
//		logger.info("getConfigurations method param1(Class clazz) {} param2(String fieldName) {},param3(String type) {}, Optional<List<SequenceInfo>> {}",entityName,fieldName,type,configurations);
//		if(configurations.isPresent()) {
//			List<SequenceInfo> objects= configurations.get();
//			return objects.stream()
//					.filter(p -> p.getType().equals(type))
//					.findFirst();
//		}
//		return Optional.empty();
//	}
//
//	/**
//	 * Should enable sequence generator.
//	 *
//	 * @param fieldName the field name
//	 * @return true, if successful
//	 */
	public boolean shouldEnableSequenceGenerator(String entityName, String fieldName, String data) {
		Assert.hasLength(fieldName, "Illegal fieldName passed in method argument for  "+fieldName);
		Assert.notNull(entityName,"Illegal entity name passed in method : "+entityName);
		try{
			JsonNode fields = getMetaConfigurations(METADATA_DOMAIN_NAME, entityName);
			if(JSONUtils.isNull(fields)){
				logger.info("No configuration found for field {}",entityName);
				return false;
			}
			for (JsonNode field : fields) {
				if (com.salescode.channelkart.utils.StringUtils.isEqual(String.valueOf(field.get("fieldName").textValue()), fieldName, true)) {
					return SequenceGenerator.shouldModify(data);
				}
			}
		} catch(Exception e1) {
			logger.error("Exception arised while checking sequence generator's method shouldEnabled. Reason : {}", e1.getMessage(),e1);
		}
		return false;
	}
//
//	/**
//	 * Checks if the 'data' of 'field' matches sequence generator pattern or not.
//	 * @param entityName Name of entity for which the field and data is provided
//	 * @param fieldName The property name of entity
//	 * @param data The value of property
//	 * @return boolean value
//	 */
//	public boolean matchesSequencePattern(String entityName, String fieldName, String data){
//		Assert.hasLength(fieldName, "Illegal fieldName passed in method argument for  "+fieldName);
//		Assert.notNull(entityName,"Illegal entity name passed in method : "+entityName);
//		try{
//			JsonNode fields = getMetaConfigurations(METADATA_DOMAIN_NAME, entityName);
//			if(JSONUtils.isNull(fields)){ return false; }
//			for (JsonNode field : fields) {
//				if (com.applicate.services.channelkart.utils.StringUtils.isEqual(String.valueOf(field.get("fieldName").textValue()), fieldName, true)) {
//					if(field.has("appPattern")){
//						return SequenceGenerator.matchesPattern(data, String.valueOf(field.get("appPattern").textValue()));
//					} else{
//						return SequenceGenerator.matchesPattern(data, null);
//					}
//				}
//			}
//		} catch(Exception e1) {
//			logger.error("Exception raised while checking sequence generator's method shouldEnabled. Reason : {}", e1.getMessage(),e1);
//		}
//		return false;
//	}
//
//	/**
//	 * Gets the repository.
//	 *
//	 * @return the repository
//	 */
//	@Override
//	public SequenceInfoRepository getRepository() {
//		return (SequenceInfoRepository) super.repository;
//	}
//
//	/**
//	 * Clear cache.
//	 *
//	 * @param event the event
//	 */
//	@Async
//	@EventListener
//	public void clearCache(AppCacheEvent<SequenceInfoService> event) {
//		CacheOperationsConstant operation= event.getType();
//		if(operation.equals(CacheOperationsConstant.DELETE)) {
//			logger.info("[lob:{}]Clearing cache metadata domain: {}",event.getLob(),DOMAIN_NAME);
//			AppCacheManager.getInstance().clearCache(event.getLob(), DOMAIN_NAME);
//		}
//	}
//
//	/**
//	 * Gets the sequence number.
//	 *
//	 * @param fieldName the field name
//	 * @return the sequence number
//	 * @throws Exception the exception
//	 */
//	public Integer getSequenceNumber(final String entityName, final String fieldName, final String type){
//		Assert.notNull(entityName,"Illegal entityName passed in method argument : "+entityName);
//		Assert.hasLength(fieldName, "Illegal fieldName passed in method argument for "+entityName);
//		Assert.notNull(type,"Illegal cdm argument passed in method");
//
//		loggerDebug("============================ SEQUENCE INFO ===============================");
//
//		AtomicInteger val= new AtomicInteger();
//		GlobalLock.withLock((entityName+fieldName), k->{
//			SequenceInfo sequenceInfo= setSequenceInfo(entityName, fieldName, type);
//			loggerDebug("============================ BEFORE SAVE : SEQUENCE INFO ===============================");
//
//			super.save(sequenceInfo);
//			loggerDebug("============================ AFTER SAVE : SEQUENCE INFO ===============================");
//
//			val.set(sequenceInfo.getCurrentValue());
//
//		});
//		return val.get();
//
//	}
//
//	public String getNextSequence(final String entityName, final String fieldName){
//		return getNextSequence(entityName,fieldName,new HashMap<>(1));
//	}
//
//	public String getNextSequence(final String entityName, final String fieldName,final Map<String,String> paramMap) {
//		Assert.notNull(entityName, "Illegal entityName passed in method argument : " + entityName);
//		Assert.hasLength(fieldName, "Illegal fieldName passed in method argument for " + entityName);
//		MetaData metaData = metadataService.fetchByValue(entityName, fieldName, true);
//		ObjectNode config = NullUtils.isNull(metaData) ? JSONUtils.getObjectMapper().createObjectNode() : (ObjectNode) metaData.getDomainValues().get(0);
//		config.put("type", replaceDynamicKeys(config.get("type").asText(), paramMap));
//		return GlobalLock.withLock1((entityName + fieldName), k -> {
//			SequenceInfo sequenceInfo = setSequenceInfo(entityName, fieldName, config);
//			loggerDebug("============================ BEFORE SAVE : SEQUENCE INFO ===============================");
//
//			super.save(sequenceInfo);
//			loggerDebug("============================ AFTER SAVE : SEQUENCE INFO ===============================");
//			paramMap.put("seq", String.format(String.format("%%0%dd", Integer.valueOf(defaultValueSetter(config, SEQUENCE_LENGTH, "7"))), sequenceInfo.getCurrentValue()));
//			paramMap.put("type", sequenceInfo.getType());
//			return generatePattern(defaultValueSetter(config, "pattern"), paramMap);
//		});
//	}
//
//	/**
//	 * Sets the sequence info.
//	 *
//	 * @param fieldName the field name
//	 * @param type the type
//	 * @return the sequence info
//	 * @throws ConfigurationException the configuration exception
//	 */
//	private SequenceInfo setSequenceInfo(final String entityName, final String fieldName, final String type) {
//		SequenceInfo sequenceInfo= getRepository().findByEntityAndFieldNameAndType(entityName,fieldName,type);
//		if(logger.isDebugEnabled()) {
//			logger.debug("============================ READ : SEQUENCE INFO ===============================");
//		}
//		if(sequenceInfo == null) {
//			Optional<SequenceInfo> safeConfigurations= this.getConfiguration(entityName, fieldName,type);
//			if(safeConfigurations.isPresent()) {
//				SequenceInfo sinfo= safeConfigurations.get();
//				sequenceInfo= sinfo;
//			}else {
//				throw new CustomRuntimeException("Configuration not found to generate sequence info");
//			}
//		}else {
//			sequenceInfo.setCurrentValue(sequenceInfo.getCurrentValue() + sequenceInfo.getIncrementValue());
//		}
//		return sequenceInfo;
//
//	}
//
//	private SequenceInfo setSequenceInfo(final String entityName, final String fieldName,final JsonNode config) {
//		String pattern = defaultValueSetter(config,"pattern");
//		String startValue = defaultValueSetter(config,"startValue","1");
//		String incrementValue = defaultValueSetter(config,"incrementValue","1");
//		String autoGenerateSequenceInfo = defaultValueSetter(config,"autoGenerateSequenceInfo","false");
//    	LocalDate date = LocalDate.now(getClientTimeZone());
//    	DateTimeFormatter df = DateTimeFormatter.ofPattern(config.get("type").asText());
//	    String formattedType = date.format(df);
//		SequenceInfo repsequenceInfo= getRepository().findByEntityAndFieldNameAndType(entityName,fieldName,formattedType);
//		SequenceInfo sequenceInfo = repsequenceInfo != null ? repsequenceInfo : new SequenceInfo();
//		loggerDebug("============================ READ : SEQUENCE INFO ===============================");
//
//		if(repsequenceInfo != null ) {
//			sequenceInfo.setCurrentValue(sequenceInfo.getCurrentValue() + sequenceInfo.getIncrementValue());
//
//		}else if(autoGenerateSequenceInfo.equalsIgnoreCase("true")) {
//					sequenceInfo.setEntity(entityName);
//					sequenceInfo.setFieldName(fieldName);
//					sequenceInfo.setType(formattedType);
//					sequenceInfo.setPattern(pattern);
//					sequenceInfo.setCurrentValue(Integer.valueOf(startValue));
//					sequenceInfo.setIncrementValue(Integer.valueOf(incrementValue));
//
//			}else {
//					throw new CustomRuntimeException("Configuration not found to generate sequence info");
//		}
//
//		return sequenceInfo;
//
//	}
//	private ZoneId getClientTimeZone() {
//		String lob = SecurityContextUtils.getLob();
//		CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator
//				.lookup(CustomerAccountInfo.class);
//		String timeZoneStr = customerService.getTimeZone(lob);
//		timeZoneStr = timeZoneStr == null ? "Asia/Kolkata" : timeZoneStr;
//		return ZoneId.of(timeZoneStr);
//	}
//
//
//	/**
//	 * Gets the sequence number.
//	 *
//	 * @param fieldName the field name
//	 * @return the sequence number
//	 * @throws Exception the exception
//	 */
//	public Integer getSequenceNumber(final String entityName, final String fieldName){
//		return getSequenceNumber(entityName, fieldName, TYPE);
//	}
//
//	private void loggerDebug(String msg) {
//		if(logger.isDebugEnabled()) {
//			logger.debug(msg);
//		}
//	}
//
//
//
//	public String generatePattern(String pattern,Map<String,String> config ) {
//		return replaceDynamicKeys(pattern,config);
//	}
//
//	private String replaceDynamicKeys(String string, Map<String, String> params) {
//	        TemplateEngine templateEngine = SpringContext.getBean(TemplateEngine.class);
//	        return templateEngine.applyInline(string, params);
//	}
//
//	private String defaultValueSetter(JsonNode config , String key) {
//		if(config.has(key)) {
//			return config.get(key).asText();
//		}else {
//			return "";
//		}
//
//	}
//
//	private String defaultValueSetter(JsonNode config , String key, String defaultValue) {
//		if(config.has(key)) {
//			return config.get(key).asText();
//		}else {
//			return defaultValue;
//		}
//
//	}
//
//	public String generateSalescodeId(String type) {
//		String nextVal = executeSequenceProcedure(type);
//		JsonNode config = getSequenceConfig(type);
//		if (nextVal == null) {
//			int startValue = config.has("sequenceStartValue") ? config.get("sequenceStartValue").asInt() : DEFAULT_VALUE_FOR_SEQUENCE_START_VALUE;
//			createSequenceProcedure(type, startValue);
//			nextVal = executeSequenceProcedure(type);
//			if (nextVal == null)
//				throw new ResourceNotFoundException("Next sequence value can not be null, please check for {} sequence", type);
//		}
//		nextVal = prepareSequence(nextVal, config);
//		return nextVal;
//	}
//
//	private JsonNode getSequenceConfig(String type) {
//		MetaData metaData = metadataService.fetchByValue("entity", "sequenceConfig", true);
//		JsonNode config;
//		if (metaData == null || !metaData.getDomainValues().get(0).has(type)) {
//			config = JSONUtils.getObjectMapper().createObjectNode();
//		} else {
//			config = metaData.getDomainValues().get(0).get(type);
//		}
//		return config;
//	}
//
//	private String prepareSequence(String nextVal, JsonNode config) {
//		int asInt = config.has(SEQUENCE_LENGTH) ? config.get(SEQUENCE_LENGTH).asInt() : DEFAULT_VALUE_FOR_SEQUENCE_LENGTH;
//		nextVal = "0".repeat(asInt - nextVal.length() < 0 ? 0 : asInt - nextVal.length()) + nextVal;
//		if (config.has("sequencePrefix")) {
//			nextVal = config.get("sequencePrefix").asText() + nextVal;
//		}
//		if (config.has("sequenceSuffix")) {
//			nextVal = nextVal + config.get("sequenceSuffix").asText();
//		}
//		return nextVal;
//	}
//
//	public String executeSequenceProcedure(String sequenceName) {
//		return sequenceInfoRepository.executeSequenceProcedures(sequenceName);
//	}
//
//	public void createSequenceProcedure(String sequenceName, long startVal) {
//		sequenceInfoRepository.createSequenceProcedure(sequenceName, startVal);
//	}

}
