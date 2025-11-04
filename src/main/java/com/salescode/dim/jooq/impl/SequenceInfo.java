/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.dim.jooq.impl;


import com.applicate.services.channelkart.utils.EntityUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;




public class SequenceInfo extends com.salescode.dim.jooq.generated.tables.pojos.SequenceInfo implements Serializable {

	private static final long serialVersionUID = 891878480438185197L;

	private static final Logger logger = LoggerFactory.getLogger(SequenceInfo.class);


	private String entity;

	private Integer currentValue = 1;

	private Integer incrementValue = 1;


	private String fieldName;


	private String pattern="%d";


	private String type= NONE;

	private static final String NONE = "none";


	public String getEntity() {
		return entity;
	}


	public void setEntity(String entity) {
		this.entity = entity;
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




	public String getPattern() {
		return pattern;
	}



	public String getFieldName() {
		return fieldName;
	}


	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}


	public String getType() {
		if(StringUtils.isBlank(this.type)) {
			this.type= NONE;
		}
		return type;
	}




	public boolean isValid() {
		try {
			boolean validity= ((StringUtils.isNotBlank(this.entity)) &&
					(StringUtils.isNotBlank(this.fieldName) && EntityUtils.getInstance().findField(Class.forName(this.entity), this.fieldName) != null)
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
