package com.salescode.channelkart.models;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.annotation.UniqueKey;
import com.salescode.channelkart.converters.JSONArrayConverter;

import javax.persistence.*;


@Entity
@Table(name = "ck_metadata",
uniqueConstraints = @UniqueConstraint(name="uk_metadata",columnNames = {"domainName","domainType"}))
public class MetaData extends CommonDataModel {

	private static final long serialVersionUID = 1L;

	@UniqueKey
	@Column(nullable = false)
	private String domainName;
	@UniqueKey
	@Column(nullable = false)
	private String domainType;
	private String description;
	
	@Column(columnDefinition = "json", nullable = false)
	@Convert(converter = JSONArrayConverter.class)
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
