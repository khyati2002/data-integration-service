/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.component.model.SequenceGenerator;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.models.SequenceInfo;
import com.applicate.services.channelkart.repository.SequenceInfoRepository;
import com.applicate.services.channelkart.utils.JSONUtils;

import org.apache.commons.lang3.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SequenceInfoService extends AbstractCDMService<SequenceInfo> {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SequenceInfoService.class);

    /**
     * The Constant DOMAIN_NAME.
     */
    private static final String DOMAIN_NAME = "sequence";

    /**
     * The Constant DOMAIN_TYPE.
     */
    private static final String DOMAIN_TYPE = "generator";

    /**
     * The Constant CONFIGURATION_KEY.
     */
    private static final String CONFIGURATION_KEY = "configurations";
    /**
     * The Constant DEFAULT_TYPE.
     */
    private static final String TYPE = "none";
    private static final String METADATA_DOMAIN_NAME = "sequenceGenerator";
    private static final String SEQUENCE_LENGTH = "sequenceLength";
    private static final int DEFAULT_VALUE_FOR_SEQUENCE_LENGTH = 7;
    private static final int DEFAULT_VALUE_FOR_SEQUENCE_START_VALUE = 1;
    /**
     * The metadata service.
     */
    private final MetaDataService metadataService;
    private final SequenceInfoRepository sequenceInfoRepository;


    /**
     * Instantiates a new sequence info service.
     *
     * @param sequenceInfoRepository the repository
     * @param metadataService        the metadata service
     */
    public SequenceInfoService(MetaDataService metadataService, SequenceInfoRepository sequenceInfoRepository) {
        super(sequenceInfoRepository);
        this.sequenceInfoRepository = sequenceInfoRepository;
        this.metadataService = metadataService;
    }


    public JsonNode getMetaConfigurations(String domainName, String domainType) {
        MetaData sequenceGenerator = metadataService.fetchByValue(domainName, domainType);
        if (ObjectUtils.isEmpty(sequenceGenerator)) {
            if (logger.isDebugEnabled()) {
                logger.info("sequenceGenerator configuration not found for domainName: {}, domainType: {}", domainName, domainType);
            }
            return null;
        }
        if (sequenceGenerator.getDomainValues().isEmpty() || !sequenceGenerator.getDomainValues().get(0).has("fields")) {
            logger.info("Configuration not found for sequenceGenerator");
            return null;
        }
        JsonNode fields = JSONUtils.toJsonNode(sequenceGenerator.getDomainValues().get(0).get("fields"));
        if (JSONUtils.isNull(fields)) {
            logger.info("Key : \"field\" not found in configuration");
            return null;
        }
        return fields;
    }


    public boolean shouldEnableSequenceGenerator(String entityName, String fieldName, String data) {
        Assert.hasLength(fieldName, "Illegal fieldName passed in method argument for  " + fieldName);
        Assert.notNull(entityName, "Illegal entity name passed in method : " + entityName);
        try {
            JsonNode fields = getMetaConfigurations(METADATA_DOMAIN_NAME, entityName);
            if (JSONUtils.isNull(fields)) {
                logger.info("No configuration found for field {}", entityName);
                return false;
            }
            for (JsonNode field : fields) {
                if (StringUtils.isEqual(String.valueOf(field.get("fieldName").textValue()), fieldName, true)) {
                    return SequenceGenerator.shouldModify(data);
                }
            }
        } catch (Exception e1) {
            logger.error("Exception arised while checking sequence generator's method shouldEnabled. Reason : {}", e1.getMessage(), e1);
        }
        return false;
    }

    public String generateSalescodeId(String type) {
        String nextVal = executeSequenceProcedure(type);
        JsonNode config = getSequenceConfig(type);
        if (nextVal == null) {
            int startValue = config.has("sequenceStartValue") ? config.get("sequenceStartValue").asInt() : DEFAULT_VALUE_FOR_SEQUENCE_START_VALUE;
            createSequenceProcedure(type, startValue);
            nextVal = executeSequenceProcedure(type);
            if (nextVal == null){
              //  throw new Exception("Next sequence value can not be null, please check for {} sequence");
            }

        }
        nextVal = prepareSequence(nextVal, config);
        return nextVal;
    }

    public String executeSequenceProcedure(String sequenceName) {
        return sequenceInfoRepository.executeSequenceProcedures(sequenceName);
    }

    private JsonNode getSequenceConfig(String type) {
        MetaData metaData = metadataService.fetchByValue("entity", "sequenceConfig", true);
        JsonNode config;
        if (metaData == null || !metaData.getDomainValues().get(0).has(type)) {
            config = JSONUtils.getObjectMapper().createObjectNode();
        } else {
            config = metaData.getDomainValues().get(0).get(type);
        }
        return config;
    }

    public void createSequenceProcedure(String sequenceName, long startVal) {
        sequenceInfoRepository.createSequenceProcedure(sequenceName, startVal);
    }

    private String prepareSequence(String nextVal, JsonNode config) {
        int asInt = config.has(SEQUENCE_LENGTH) ? config.get(SEQUENCE_LENGTH).asInt() : DEFAULT_VALUE_FOR_SEQUENCE_LENGTH;
        nextVal = "0".repeat(asInt - nextVal.length() < 0 ? 0 : asInt - nextVal.length()) + nextVal;
        if (config.has("sequencePrefix")) {
            nextVal = config.get("sequencePrefix").asText() + nextVal;
        }
        if (config.has("sequenceSuffix")) {
            nextVal = nextVal + config.get("sequenceSuffix").asText();
        }
        return nextVal;
    }


}
