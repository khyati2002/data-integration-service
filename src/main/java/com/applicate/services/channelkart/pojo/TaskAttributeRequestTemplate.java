/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.pojo;

import com.applicate.services.channelkart.services.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * The class TaskAttributeRequestTemplate.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskAttributeRequestTemplate {

    /** The entity name. */
    private String entityName;

    /** The file path. */
    private String filePath;

    /** The field name. */
    private String fieldName;

    /** The criteria. */
    private Map<String,String> criteria;

    /** The lob. */
    private String lob;

    /** The type. */
    private String type;

    private Collection<TransformerInfo> transformerInfo;

    /**
     * The class TransformerInfo.
     *
     * @author Manish Srivastava
     * @since  Aug 2020
     */
    public static class TransformerInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        public boolean isSkipPreprocessing() {
            return skipPreprocessing;
        }

        public void setSkipPreprocessing(boolean skipPreprocessing) {
            this.skipPreprocessing = skipPreprocessing;
        }

        private boolean skipPreprocessing;

        private boolean skipPersist;

        /** The entity name. */
        private String entityName;

        /** The transformer id. */
        private String transformerId;

        /** The Preprocess Validation Exclude Group Name */
        private String preprocessValidationExcludeGroup;

        public String getMessageLevelHash() {
            return messageLevelHash;
        }

        public void setMessageLevelHash(String messageLevelHash) {
            this.messageLevelHash = messageLevelHash;
        }

        private String messageLevelHash;

        public boolean getMessageHashSupported() {
            return messageHashSupported;
        }

        public void setMessageHashSupported(boolean messageHashSupported) {
            this.messageHashSupported = messageHashSupported;
        }

        private boolean messageHashSupported;

        private String messageLevelKey;

        public String getMessageLevelKey() {
            return messageLevelKey;
        }

        public void setMessageLevelKey(String messageLevelKey) {
            this.messageLevelKey = messageLevelKey;
        }

        public String[] getCachedArtifact() {
            return cachedArtifact;
        }

        public void setCachedArtifact(String[] cachedArtifact) {
            this.cachedArtifact = cachedArtifact;
        }

        private String[] cachedArtifact;


        private com.applicate.services.channelkart.transformers.TransformerInfo transformer;

        public com.applicate.services.channelkart.transformers.TransformerInfo getTransformer() {
            return transformer;
        }

        public void setTransformer(
                com.applicate.services.channelkart.transformers.TransformerInfo transformer) {
            this.transformer = transformer;
        }

        /** The operation type. */
        private OperationType operationType= OperationType.insert;

        /**
         * @return the operationType
         */
        public OperationType getOperationType() {
            return operationType;
        }

        /**
         * @param operationType the operationType to set
         */
        public void setOperationType(OperationType operationType) {
            this.operationType = operationType;
        }

        /**
         * @return the entityName
         */
        public String getEntityName() {
            return entityName;
        }

        /**
         * @param entityName the entityName to set
         */
        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        /**
         * @return the transformerId
         */
        public String getTransformerId() {
            return transformerId;
        }

        /**
         * @param transformerId the transformerId to set
         */
        public void setTransformerId(String transformerId) {
            this.transformerId = transformerId;
        }

        public String getPreprocessValidationExcludeGroup() {
            return preprocessValidationExcludeGroup;
        }

        public void setPreprocessValidationExcludeGroup(String preprocessValidationExcludeGroup) {
            this.preprocessValidationExcludeGroup = preprocessValidationExcludeGroup;
        }

        public boolean isSkipPersist() {
            return skipPersist;
        }

        public void setSkipPersist(boolean skipPersist) {
            this.skipPersist = skipPersist;
        }
    }

    /**
     * Gets the criteria.
     *
     * @return the criteria
     */
    public Map<String,String> getCriteria() {
        return criteria;
    }

    /**
     * Sets the criteria.
     *
     * @param criteria the criteria
     */
    public void setCriteria(Map<String,String> criteria) {
        this.criteria = criteria;
    }

    /**
     * Gets the entity name.
     *
     * @return the entity name
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Sets the entity name.
     *
     * @param entityName the new entity name
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * Gets the file path.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets the file path.
     *
     * @param filePath the new file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the field name.
     *
     * @param fieldName the new field name
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getLob() {
        return lob;
    }

    /**
     * Sets the type.
     *
     * @param lob the new lob
     */
    public void setLob(String lob) {
        this.lob = lob;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the transformerInfo
     */
    public Collection<TransformerInfo> getTransformerInfo() {
        return transformerInfo;
    }

    /**
     * @param transformerInfo the transformerInfo to set
     */
    public void setTransformerInfo(Collection<TransformerInfo> transformerInfo) {
        this.transformerInfo = transformerInfo;
    }

}
