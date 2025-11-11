package com.salescode.dim.jooq.impl;

import java.io.Serializable;

public class SequenceInfo extends com.salescode.dim.jooq.generated.tables.pojos.SequenceInfo implements Serializable {
	private String id;
	private String entity;
	private String fieldName;
	private Integer currentValue;
	private Integer incrementValue;
	private String pattern;
	private String type;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
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

	public void setIncrementValue(Integer incrementValue) {
		this.incrementValue = incrementValue;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
