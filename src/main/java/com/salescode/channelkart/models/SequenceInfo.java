/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.salescode.channelkart.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescode.channelkart.annotation.UniqueKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.salescode.channelkart.utils.EntityUtils;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * The class SequenceInfo.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@Entity
@Table(name="ck_sequence_info", indexes={@Index(name="ck_sequence_info_idx_1",columnList="entity, fieldName"),
		@Index(name="ck_sequence_info_idx_2",columnList="entity, fieldName, type")},
uniqueConstraints = @UniqueConstraint(name="uk_sequence_info",columnNames = {"entity","fieldName","type"}))
@JsonIgnoreProperties(ignoreUnknown = true)
public class SequenceInfo extends CommonDataModel {

	/** The Constant serialVersionUID. */
	@Transient
	private static final long serialVersionUID = 891878480438185197L;
	
	/** The Constant logger. */
	@Transient
	private static final Logger logger = LoggerFactory.getLogger(SequenceInfo.class);

	/** The entity name. */
	@NotNull
	@NotBlank
	@UniqueKey
	private String entity;

	/** The next value. */
	private Integer currentValue = 1;

	/** The increment value. */
	private Integer incrementValue = 1;

	/** The field name. */
	@NotNull
	@NotBlank
	@UniqueKey
	private String fieldName;
	
	/** The pattern. */
	@NotNull
	@NotBlank
	private String pattern="%d";
	
	/** The type. */
	@NotBlank
	@UniqueKey
	@Column(columnDefinition = "varchar(100)")
	private String type= NONE;
	
	/** The Constant TYPE. */
	@Transient
	@JsonIgnore
	private static final String NONE = "none";
	
	/**
	 * @return the entity
	 */
	public String getEntity() {
		return entity;
	}

	/**
	 * @param entity the entity to set
	 */
	public SequenceInfo setEntity(String entity) {
		this.entity = entity;
		return this;
	}

	/**
	 * @return the currentValue
	 */
	public Integer getCurrentValue() {
		return currentValue;
	}

	/**
	 * @param currentValue the currentValue to set
	 */
	public void setCurrentValue(Integer currentValue) {
		this.currentValue = currentValue;
	}

	/**
	 * Gets the increment value.
	 *
	 * @return the incrementValue
	 */
	public Integer getIncrementValue() {
		return incrementValue;
	}

	/**
	 * Sets the increment value.
	 *
	 * @param incrementValue the incrementValue to set
	 */
	public SequenceInfo setIncrementValue(Integer incrementValue) {
		this.incrementValue = incrementValue;
		return this;
	}

	/**
	 * Gets the pattern.
	 *
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Sets the pattern.
	 *
	 * @param pattern the pattern to set
	 */
	public SequenceInfo setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @param fieldName the fieldName to set
	 */
	public SequenceInfo setFieldName(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		if(StringUtils.isBlank(this.type)) {
			this.type= NONE;
		}
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public SequenceInfo setType(String type) {
		if(StringUtils.isNotBlank(type)) {
			this.type = type;
		}
		return this;
	}

	/**
	 * Checks if is valid.
	 *
	 * @return true, if is valid
	 */
//	public boolean isValid() {
//		try {
//			boolean validity= ((StringUtils.isNotBlank(this.entity)) &&
//					(StringUtils.isNotBlank(this.fieldName) && EntityUtils.get().findField(Class.forName(this.entity), this.fieldName) != null)
//					&& (StringUtils.isNotBlank(this.pattern) && this.incrementValue > 0 ));
//			if(!validity && logger.isWarnEnabled()) {
//				logger.warn("Found invalid configuration for {}",this.toString());
//			}
//			return validity;
//		}catch(ClassNotFoundException | RuntimeException ex) {
//			logger.error("Found invalid configuration for {}, invalid entity : {}",this.toString(), this.getEntity());
//            return false;
//		}
//	}
	
	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
		return result;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
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

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "SequenceNumber [entity=" + entity + ", currentValue=" + currentValue + ", incrementValue="
				+ incrementValue + ", fieldName=" + fieldName + ", pattern=" + pattern + "]";
	}
	
}
