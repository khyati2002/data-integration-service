/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.component.model.SequenceGenerator;
import com.salescode.channelkart.repository.SequenceInfoRepository;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.generated.tables.pojos.CkSequenceInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SequenceInfoService extends AbstractCDMService<CkSequenceInfo> {

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
        this.sequenceInfoRepository = sequenceInfoRepository;
        this.metadataService = metadataService;
    }


    public JsonNode getMetaConfigurations(String domainName, String domainType) {
        CkMetadata sequenceGenerator = metadataService.fetchByValue(domainName, domainType);
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
                if (com.salescode.channelkart.utils.StringUtils.isEqual(String.valueOf(field.get("fieldName").textValue()), fieldName, true)) {
                    return SequenceGenerator.shouldModify(data);
                }
            }
        } catch (Exception e1) {
            logger.error("Exception arised while checking sequence generator's method shouldEnabled. Reason : {}", e1.getMessage(), e1);
        }
        return false;
    }


}
