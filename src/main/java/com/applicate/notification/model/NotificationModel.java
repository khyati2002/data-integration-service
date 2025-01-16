package com.applicate.notification.model;

import com.applicate.notification.NotificationTypeRegistry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("rawtypes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationModel {

    private NotificationTypeRegistry type;

	private String profileName;

	private String[] recipient;
	
	private NotificationPayload payload;

	private String lob;

	private String templateUrl;

	private String source;
	
	private JsonNode recipientInfo;
	
	private String groupKey;
	
	private Map<String,Object> metaInfo;

	public Map<String, Object> getMetaInfo() {
		return metaInfo;
	}

	public void setMetaInfo(Map<String, Object> metaInfo) {
		this.metaInfo = metaInfo;
	}

	public JsonNode getRecipientInfo() {
		return recipientInfo;
	}

	public void setRecipientInfo(JsonNode recipientsInfo){
		this.recipientInfo = recipientsInfo;
	}
	
	public NotificationPayload getPayload() {
		return payload;
	}

	public void setPayload(NotificationPayload payload){
		this.payload = payload;
	}

	public String[] getRecipient() {
		return recipient;
	}

	public void setRecipient(String[] recipient) {
		this.recipient = recipient;
	}

	public String getLob() {
		return lob;
	}

	public void setLob(String lob) {
		this.lob = lob;
	}

	public NotificationTypeRegistry getType() {
		return type;
	}

	public void setType(NotificationTypeRegistry type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "NotificationModel [recipient=" + Arrays.toString(recipient)+",recipientsInfo="+recipientInfo+ ", lob=" + lob  + ", payload=" + payload + "]";
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getTemplateUrl() {
		return templateUrl;
	}

	public void setTemplateUrl(String templateUrl) {
		this.templateUrl = templateUrl;
	}

	public String getSource() {
		return source;
	}

	public NotificationModel setSource(String source) {
		this.source = source;
		return this;
	}

	public String getGroupKey() {
		return groupKey;
	}

	public NotificationModel setGroupKey(String groupKey) {
		this.groupKey = groupKey;
		return this;
	}
}
