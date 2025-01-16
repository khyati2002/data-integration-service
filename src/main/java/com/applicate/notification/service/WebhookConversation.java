/**
 * Copyright Applicate(2021) To Present
 * 
 * All rights reserved
 */
package com.applicate.notification.service;


/**
 * The Interface WebhookConversation.
 *
 * @author Manish Srivastava
 * @since  Apr 2021
 */
public interface WebhookConversation {

	/**
	 * Response.
	 *
	 * @param data the data
	 * @return the string
	 */
	public String response(WebhookConversationDTO data);
	
}
