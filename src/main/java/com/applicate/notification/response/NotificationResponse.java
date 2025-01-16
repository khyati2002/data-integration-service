package com.applicate.notification.response;

import com.applicate.notification.model.NotificationModel;
import com.applicate.notification.model.NotificationStatus;
import com.applicate.services.channelkart.response.OperationStatus;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.*;

public class NotificationResponse {

	private static final String COUNT_TAG = "number";

	private static final int DEFAULT_COUNT = 1;

	private OperationStatus status;

	private Map<String, Object> response;

	private NotificationModel notificationModel;

	private List<NotificationStatus> details= new ArrayList<>(); 

	private NotificationResponse(OperationStatus status) {
		this.status = status;
		withCount(DEFAULT_COUNT)
		.withServiceResponse(status.isSuccess() ? "success" : "failure");
	}

	public boolean isFailure() {
		return !isSuccess();
	}

	public OperationStatus getStatus() {
		return status;
	}

	public boolean isSuccess() {
		return status.isSuccess();
	}

	public NotificationResponse setStatus(OperationStatus status) {
		this.status = status;
		return this;
	}

	public NotificationResponse with(String responseKey, Object responseValue) {
		getResponse().put(responseKey, responseValue);
		return this;
	}

	public NotificationResponse withServiceResponse(String value) {
		return with("serviceResponse", value);
	}

	public NotificationResponse withCount(int count) {
		return with(COUNT_TAG, count);
	}

	public NotificationResponse with(Map<String, String> attributes) {
		getResponse().putAll(attributes);
		return this;
	}

	public NotificationResponse putIfAbsent(String responseKey, Object responseValue) {
		getResponse().putIfAbsent(responseKey, responseValue);
		return this;
	}

	public Map<String, Object> getResponse() {
		if (this.response == null) {
			this.response = new HashMap<>();
		}
		if (this.getDetails() != null) {
			this.response.put("details", getDetails());
		}
		return response;
	}

	public NotificationResponse setResponse(Map<String, Object> response) {
		this.response = response;
		return this;
	}

	public NotificationModel getNotificationModel() {
		return notificationModel;
	}

	public boolean hasNotificationModel() {
		return this.notificationModel != null;
	}

	public NotificationResponse setNotificationModel(NotificationModel notificationModel) {
		this.notificationModel = notificationModel;
		return this;
	}

	public JsonNode toJson() {
		Map<String, Object> responseDetails = new HashMap<>(getResponse());
		responseDetails.putIfAbsent("status", this.status.name());
		if (hasNotificationModel()) {
			responseDetails.putIfAbsent("type", notificationModel.getProfileName());
			responseDetails.putIfAbsent("profile", notificationModel.getProfileName());
			String source = notificationModel.getSource();
			responseDetails.putIfAbsent("source", source != null ? source : "unknown");
		}
		return JSONUtils.toJsonNode(responseDetails);
	}

	public static NotificationResponse fromException(Exception e) {
		Map<String, Object> response = new HashMap<>();
		response.put("exception", e.getClass().getName());
		response.put("message", ExceptionUtils.getMessage(e));
		return new NotificationResponse(OperationStatus.Failure)
				.setResponse(response);
	}

	public static NotificationResponse fromStatus(OperationStatus status) {
		return new NotificationResponse(status);
	}

	@Override
	public String toString() {
		return "NotificationResponse{" +
				"status=" + status +
				", response=" + response +
				", notificationModel=" + notificationModel +
				'}';
	}

	public String getEssentialDetails() {
		return "status=" + status +
				", response=" + ((response == null) ? "" : response.get("message")) +
				", recipients=" + getRecipientsAsString();
	}

	private String getRecipientsAsString() {
		return hasNotificationModel()
				? Arrays.toString(notificationModel.getRecipient())
						: "null";
	}

	/**
	 * @return the details
	 */
	public List<NotificationStatus> getDetails() {
		return details;
	}

	/**
	 * @param details the details to set
	 */
	public NotificationResponse setDetails(List<NotificationStatus> details) {
		this.details = details;
		return this;
	}
	
	public NotificationResponse withDetails(List<NotificationStatus> details) {
		getDetails().addAll(details);
		return this;
	}
 
}
