package com.applicate.services.channelkart.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.annotation.UniqueKey;
import com.applicate.services.channelkart.converters.JSONObjectConverter;
import com.applicate.services.channelkart.security.vault.VaultManager;
import com.applicate.services.channelkart.utils.EntityUtils;
import lombok.Getter;
import lombok.Setter;

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

	@Setter
    @Getter
    @UniqueKey
	@Column(unique = true)
	private String name;
	
	@Setter
    @Getter
    private String type;
	
	@Column(columnDefinition = "json")
	@Convert(converter= JSONObjectConverter.class)
	private JsonNode attributes;
	
	@Setter
    @Getter
    @Column(columnDefinition = "longtext")
	private String payload;

	@Getter
    private String implementation;

    public Profile() {
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
