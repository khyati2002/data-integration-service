/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.cache.AppCacheManager;
import com.applicate.services.channelkart.component.model.SequenceGenerator;
import com.applicate.services.channelkart.repository.SequenceInfoRepository;


import com.applicate.services.channelkart.utils.GlobalLock;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.impl.SequenceInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SequenceInfoService extends AbstractCDMService<SequenceInfo>{

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(SequenceInfoService.class);

	/** The Constant DEFAULT_TYPE. */
	private static final String TYPE= "none";

    /** The Constant DOMAIN_NAME. */
    private static final String DOMAIN_NAME = "sequence";

    /** The Constant DOMAIN_TYPE. */
    private static final String DOMAIN_TYPE = "generator";

    /** The Constant CONFIGURATION_KEY. */
    private static final String CONFIGURATION_KEY = "configurations";

    private static final String METADATA_DOMAIN_NAME= "sequenceGenerator";




    private final SequenceInfoRepository sequenceInfoRepository;

    private final MetaDataService metaDataService;




	public SequenceInfoService(SequenceInfoRepository sequenceInfoRepository, MetaDataService metaDataService) {
		this.sequenceInfoRepository = sequenceInfoRepository;
        this.metaDataService=metaDataService;
	}


	public Integer getSequenceNumber(final String entityName, final String fieldName, final String type){

		loggerDebug("============================ SEQUENCE INFO ===============================");

		AtomicInteger val= new AtomicInteger();
		GlobalLock.withLock((entityName+fieldName), k->{
			SequenceInfo sequenceInfo= setSequenceInfo(entityName, fieldName, type);
			loggerDebug("============================ BEFORE SAVE : SEQUENCE INFO ===============================");

			super.save(sequenceInfo);
			loggerDebug("============================ AFTER SAVE : SEQUENCE INFO ===============================");

			val.set(sequenceInfo.getCurrentValue());

		});
		return val.get();

	}



	private SequenceInfo setSequenceInfo(final String entityName, final String fieldName, final String type) {
		SequenceInfo sequenceInfo= sequenceInfoRepository.findByEntityAndFieldNameAndType(entityName,fieldName,type);
		if(logger.isDebugEnabled()) {
			logger.debug("============================ READ : SEQUENCE INFO ===============================");
		}
		if(sequenceInfo == null) {
			Optional<SequenceInfo> safeConfigurations= this.getConfiguration(entityName, fieldName,type);
			if(safeConfigurations.isPresent()) {
				SequenceInfo sinfo= safeConfigurations.get();
				sequenceInfo= sinfo;
			}else {
				throw new RuntimeException("Configuration not found to generate sequence info");
			}
		}else {
			sequenceInfo.setCurrentValue(sequenceInfo.getCurrentValue() + sequenceInfo.getIncrementValue());
		}
		return sequenceInfo;

	}

    public Optional<SequenceInfo> getConfiguration(String entityName, String fieldName, String type){

        Optional<List<SequenceInfo>> configurations= getConfiguration(entityName,fieldName);
        logger.info("getConfigurations method param1(Class clazz) {} param2(String fieldName) {},param3(String type) {}, Optional<List<SequenceInfo>> {}",entityName,fieldName,type,configurations);
        if(configurations.isPresent()) {
            List<SequenceInfo> objects= configurations.get();
            return objects.stream()
                    .filter(p -> p.getType().equals(type))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<List<SequenceInfo>> getConfiguration(String entityName, String fieldName){
        Optional<List<SequenceInfo>> configurations= getConfiguration(entityName);

        logger.info("getConfigurations method param1(Class clazz) {} param2(String fieldName) {}, Optional<List<SequenceInfo>> {}",entityName,fieldName,configurations);

        if(configurations.isPresent()) {
            List<SequenceInfo> result= AppCacheManager.getInstance().withCache(DOMAIN_NAME,String.format("%s:%s",entityName,fieldName),tmp->{
                List<SequenceInfo> objects= configurations.get();
                List<SequenceInfo> data= objects.stream()
                        .filter(p -> p.getFieldName().equals(fieldName))
                        .collect(Collectors.toList());
                return (ObjectUtils.isNotEmpty(data)) ? data : null;
            });
            return (result == null) ? Optional.empty() : Optional.of(result);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<SequenceInfo>> getConfiguration(String entityName){

        Object result= AppCacheManager.getInstance().withCache(DOMAIN_NAME,entityName,tmp->{
            Optional<List<SequenceInfo>> data= getConfigurations();

            logger.info("getConfigurations method param1(Class clazz) {}, Optional<List<SequenceInfo>> {}",entityName,data);

            if(data.isPresent()) {
                List<SequenceInfo> objects= data.get();
                return objects.stream()
                        .filter(p ->p.isValid() && StringUtils.endsWithIgnoreCase(p.getEntity(), entityName))
                        .collect(Collectors.toList());

            }
            return null;
        });
        return (result == null) ? Optional.empty() : Optional.of((List<SequenceInfo>)result);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<SequenceInfo>> getConfigurations(){
        Object result= AppCacheManager.getInstance().withCache(DOMAIN_NAME, CONFIGURATION_KEY, tmp->{
            Metadata metadata= metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE);

            logger.info("getConfigurations method no param, metadata {}",metadata);

            if(ObjectUtils.isNotEmpty(metadata)) {
                ArrayNode arrayNode= (ArrayNode) metadata.getDomainValues();
                if(ObjectUtils.isNotEmpty(arrayNode) && !arrayNode.isEmpty()) {
                    try {
                        return JSONUtils.getObjectMapper().readValue(arrayNode.toString(), new TypeReference<List<SequenceInfo>>(){});
                    } catch (JsonProcessingException e) {
                        logger.error("stacktrace", e);
                    }
                }
            }
            return null;
        });
        return (result == null) ? Optional.empty() : Optional.of((List<SequenceInfo>)result);
    }



	public Integer getSequenceNumber(final String entityName, final String fieldName){
		return getSequenceNumber(entityName, fieldName, TYPE);
	}

    public boolean matchesSequencePattern(String entityName, String fieldName, String data){

        try{
            JsonNode fields = getMetaConfigurations(METADATA_DOMAIN_NAME, entityName);
            if(JSONUtils.isNull(fields)){ return false; }
            for (JsonNode field : fields) {
                if (String.valueOf(field.get("fieldName").textValue()).equalsIgnoreCase(fieldName)) {
                    if(field.has("appPattern")){
                        return SequenceGenerator.matchesPattern(data, String.valueOf(field.get("appPattern").textValue()));
                    } else{
                        return SequenceGenerator.matchesPattern(data, null);
                    }
                }
            }
        } catch(Exception e1) {
            logger.error("Exception raised while checking sequence generator's method shouldEnabled. Reason : {}", e1.getMessage(),e1);
        }
        return false;
    }

    public JsonNode getMetaConfigurations(String domainName, String domainType){
        Metadata sequenceGenerator = metaDataService.fetchByValue(domainName, domainType);
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
        JsonNode fields = JSONUtils.toJsonNode((Map<?, ?>) sequenceGenerator.getDomainValues().get(0).get("fields"));
        if(JSONUtils.isNull(fields)){
            logger.info("Key : \"field\" not found in configuration");
            return null;
        }
        return fields;
    }

	private void loggerDebug(String msg) {
		if(logger.isDebugEnabled()) {
			logger.debug(msg);
		}
	}

}
