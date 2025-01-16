/**
 * Copyright Applicate(2021) To Present
 * 
 * All rights reserved
 */
package com.applicate.notification.service;

/**
 * The Class WebhookConversationDTO.
 *
 * @author Manish Srivastava
 * @since  Apr 2021
 */
public class WebhookConversationDTO {

	/** The login id. */
	private String loginId;
	
	/** The channel. */
	private String channel;
	
	/** The channel id. */
	private String channelId;
	
	/** The message. */
	private String message;
	
	/** The name. */
	private String name;
	
	/**
	 * Instantiates a new webhook conversation DTO.
	 */
	private WebhookConversationDTO() {}

	/**
	 * Gets the login id.
	 *
	 * @return the login id
	 */
	//@JsonProperty("loginid")
	public String getLoginId() {
		return loginId;
	}

	/**
	 * Sets the login id.
	 *
	 * @param loginId the login id
	 * @return the webhook conversation DTO
	 */
	public WebhookConversationDTO setLoginId(String loginId) {
		this.loginId = loginId;
		return this;
	}

	/**
	 * Gets the channel.
	 *
	 * @return the channel
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * Sets the channel.
	 *
	 * @param channel the channel
	 * @return the webhook conversation DTO
	 */
	public WebhookConversationDTO setChannel(String channel) {
		this.channel = channel;
		return this;
	}

	/**
	 * Gets the channel id.
	 *
	 * @return the channel id
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Sets the channel id.
	 *
	 * @param channelId the channel id
	 * @return the webhook conversation DTO
	 */
	public WebhookConversationDTO setChannelId(String channelId) {
		this.channelId = channelId;
		return this;
	}

	/**
	 * Gets the message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the message.
	 *
	 * @param message the message
	 * @return the webhook conversation DTO
	 */
	public WebhookConversationDTO setMessage(String message) {
		this.message = message;
		return this;	
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name
	 * @return the webhook conversation DTO
	 */
	public WebhookConversationDTO setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Creates the.
	 *
	 * @param loginId the login id
	 * @param channel the channel
	 * @param message the message
	 * @return the webhook conversation DTO
	 */
	public static WebhookConversationDTO create(String loginId, String channel, String message) {
		WebhookConversationDTO data= new WebhookConversationDTO();
		return data.setLoginId(loginId)
				.setChannel(channel)
				.setMessage(message);
	}
	
}
