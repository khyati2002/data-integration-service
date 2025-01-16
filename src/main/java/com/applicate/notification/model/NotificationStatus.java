/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.notification.model;

import com.applicate.services.channelkart.response.OperationStatus;

/**
 * The class NotificationStatus.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
public class NotificationStatus {

	/** The recipient id. */
	private String recipientId;
	
	/** The reason. */
	private String reason="NA";
	
	/** The status. */
	private OperationStatus status= OperationStatus.Failure;
	
	/** The response. */
	private String serviceResponse;

	/**
	 * Gets the recipient id.
	 *
	 * @return the recipientId
	 */
	public String getRecipientId() {
		return recipientId;
	}

	/**
	 * Sets the recipient id.
	 *
	 * @param recipientId the recipientId to set
	 * @return the notification status
	 */
	public NotificationStatus setRecipientId(String recipientId) {
		this.recipientId = recipientId;
		return this;
	}

	/**
	 * Gets the reason.
	 *
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Sets the reason.
	 *
	 * @param reason the reason to set
	 * @return the notification status
	 */
	public NotificationStatus setReason(String reason) {
		this.reason = reason;
		return this;
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public OperationStatus getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the status to set
	 * @return the notification status
	 */
	public NotificationStatus setStatus(OperationStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Gets the service response.
	 *
	 * @return the serviceResponse
	 */
	public String getServiceResponse() {
		return serviceResponse;
	}

	/**
	 * Sets the service response.
	 *
	 * @param serviceResponse the serviceResponse to set
	 */
	public void setServiceResponse(String serviceResponse) {
		this.serviceResponse = serviceResponse;
	}
	
}
