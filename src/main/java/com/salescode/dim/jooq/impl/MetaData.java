package com.salescode.dim.jooq.impl;


import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.Serializable;


public class MetaData extends com.salescode.dim.jooq.generated.tables.pojos.Metadata implements Serializable {

	private static final long serialVersionUID = 1L;


	private String domainName;

	private String domainType;
	private String description;
	

	private ArrayNode domainValues;

	public ArrayNode getDomainValues() {
		return domainValues;
	}

	public void setDomainValues(ArrayNode domainValues) {
		this.domainValues = domainValues;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getDomainType() {
		return domainType;
	}

	public void setDomainType(String domainType) {
		this.domainType = domainType;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "MetaData{" +
				"domainName='" + domainName + '\'' +
				", domainType='" + domainType + '\'' +
				", description='" + description + '\'' +
				", domainValues=" + domainValues +
				'}';
	}
}
