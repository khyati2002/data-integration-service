package com.applicate.services.channelkart.models;

import com.applicate.services.channelkart.annotation.UniqueKey;
import com.applicate.services.channelkart.converters.JSONObjectConverter;
import com.applicate.services.channelkart.security.vault.VaultManager;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;



@Entity
@Table(name="profile")
public class Profile extends CommonDataModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@UniqueKey
	@Column(unique = true)
	private String name;

	private String type;

	@Column(columnDefinition = "json")
	@Convert(converter= JSONObjectConverter.class)
	private JsonNode attributes;

	@Column(columnDefinition = "longtext")
	private String payload;

	private String implementation;

	public Profile() {
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public JsonNode getAttributes() {
		if(payload!=null) {
			return new JSONObjectConverter().convertToEntityAttribute(VaultManager.decrypt(payload));
		}
		return attributes;
	}

	public void setAttributes(JsonNode attributes) {
		this.attributes = attributes;
		if(attributes!=null) {
			this.setPayload(VaultManager.encrypt(attributes.toString()));
			this.attributes=null;
		}
	}

	public void populateAttributesOnly(JsonNode attributes){
		this.attributes = attributes;
	}

	public String getImplementation() {
		return implementation;
	}

	public Profile setImplementation(String implementation) {
		this.implementation = implementation;
		return this;
	}

	public Profile(Profile profile) {
		this.name = profile.name;
		this.type = profile.type;
		this.attributes = EntityUtils.deepClone(profile.getAttributes());
		this.payload = EntityUtils.deepClone(profile.payload);
		this.implementation = profile.implementation;
	}
}
