/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.notification.model;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * The class NotificationPayload.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@SuppressWarnings("unchecked")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationPayload<T extends NotificationPayload<T>>{

	@JsonIgnore
	private static final String EMPTY="";

	/** The notification id. */
	private String notificationId;

	/** The user name. */
	private String userName;

	/** The body. */
	private String body;

	/** The params. */
	private Map<String,Object> params;
	
	/** The subject. */
	private String subject;
	
	/** The category. */
	private String category;

	/** The cc recipients */

	private String cc;

	private String bcc;
	
	/*This is the type of notification e.g., text, video ,contact, location etc.
	 *Do not consider it as NotificationTypeRegistry constants 
	 * */
	private String notificationType;

	/**
	 * Gets the body.
	 *
	 * @return the body
	 */
	public String getBody() {
		return (this.body == null)? EMPTY : this.body;
	}

	/**
	 * Sets the body.
	 *
	 * @param body the body to set
	 */
	public T setBody(String body) {
		this.body = body;
		return (T) this;
	}

	/**
	 * Gets the params.
	 *
	 * @return the params
	 */
	public Map<String, Object> getParams() {
		return params;
	}

	/**
	 * Sets the params.
	 *
	 * @param params the params to set
	 */
	public T setParams(Map<String, Object> params) {
		this.params = params;
		return (T) this;
	}

	/**
	 * @return the notificationId
	 */
	public String getNotificationId() {
		return notificationId;
	}

	/**
	 * @param notificationId the notificationId to set
	 */
	public T setNotificationId(String notificationId) {
		this.notificationId = notificationId;
		return (T) this;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public T setUserName(String userName) {
		this.userName = userName;
		return (T) this;
	}

	@Override
	public String toString() {
		return JSONUtils.stringify(this);
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject the subject to set
	 */
	public T setSubject(String subject) {
		this.subject = subject;
		return (T) this;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public T setCategory(String category) {
		this.category = category;
		return (T) this;
	}

	/**
	 * @return the notificationType
	 */
	public String getNotificationType() {
		return notificationType;
	}

	/**
	 * @param notificationType the notificationType to set
	 */
	public T setNotificationType(String notificationType) {
		this.notificationType = notificationType;
		return (T) this;
	}

	/**
	 * @return the cc
	 */
	public String getCc() {
		return cc;
	}

	public String getBcc(){
		return bcc;
	}

	/**
	 * @param cc the cc recipients to set
	 */
	public T setCc(String cc) {
		this.cc = cc;
		return (T) this;
	}



	public T setBcc(String bcc){
		this.bcc = bcc;
		return (T) this;
	}

}
