package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceInfo extends com.salescode.dim.jooq.generated.tables.pojos.SequenceInfo {

    private static final long serialVersionUID = 891878480438185197L;

    private static final Logger logger = LoggerFactory.getLogger(SequenceInfo.class);

    private String entity;

    private Integer currentValue = 1;

    private Integer incrementValue = 1;

    private String fieldName;

    private String pattern="%d";

    @NotBlank
    private String type= NONE;

    @JsonIgnore
    private static final String NONE = "none";

    public String getEntity() {
        return entity;
    }

    public SequenceInfo setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public Integer getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
    }

    public Integer getIncrementValue() {
        return incrementValue;
    }

    public SequenceInfo setIncrementValue(Integer incrementValue) {
        this.incrementValue = incrementValue;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public SequenceInfo setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public String getFieldName() {
        return fieldName;
    }

    public SequenceInfo setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getType() {
        if(StringUtils.isBlank(this.type)) {
            this.type= NONE;
        }
        return type;
    }

    public SequenceInfo setType(String type) {
        if(StringUtils.isNotBlank(type)) {
            this.type = type;
        }
        return this;
    }

    public boolean isValid() {
        try {
            boolean validity= ((StringUtils.isNotBlank(this.entity)) &&
                    (StringUtils.isNotBlank(this.fieldName) && EntityUtils.get().findField(Class.forName(this.entity), this.fieldName) != null)
                    && (StringUtils.isNotBlank(this.pattern) && this.incrementValue > 0 ));
            if(!validity && logger.isWarnEnabled()) {
                logger.warn("Found invalid configuration for {}",this.toString());
            }
            return validity;
        }catch(ClassNotFoundException | RuntimeException ex) {
            logger.error("Found invalid configuration for {}, invalid entity : {}",this.toString(), this.getEntity());
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((entity == null) ? 0 : entity.hashCode());
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        SequenceInfo other = (SequenceInfo) obj;
        if (entity == null) {
            if (other.entity != null)
                return false;
        } else if (!entity.equals(other.entity))
            return false;
        if (fieldName == null) {
            if (other.fieldName != null)
                return false;
        } else if (!fieldName.equals(other.fieldName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SequenceNumber [entity=" + entity + ", currentValue=" + currentValue + ", incrementValue="
                + incrementValue + ", fieldName=" + fieldName + ", pattern=" + pattern + "]";
    }

}

