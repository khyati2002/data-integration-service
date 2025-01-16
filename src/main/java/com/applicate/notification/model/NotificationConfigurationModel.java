/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.notification.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The class NotificationConfigurationModel.
 *
 * @author  Manish Srivastava
 * @param   <T> the generic type
 * @since   Feb 2021
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationConfigurationModel<T extends NotificationConfigurationModel<T>> {

	/** The url. */
	private String url;
	
	/** The access token. */
	private String accessToken;
    
	/** The headers. */
	private Map<String,String> headers;
	

	/** The positive responses. */
	private List<String> positiveResponses = Arrays.asList("yes");	
	
	/**
	 * Gets the access token.
	 *
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Sets the access token.
	 *
	 * @param accessToken the accessToken to set
	 * @return the t
	 */
	public T setAccessToken(String accessToken) {
		this.accessToken = accessToken;
		return (T) this;
	}

	/**
	 * Gets the headers.
	 *
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Sets the headers.
	 *
	 * @param headers the headers to set
	 * @return the t
	 */
	public T setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return (T) this;
	}

	/**
	 * Gets the url.
	 *
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 *
	 * @param url the url to set
	 * @return the t
	 */
	public T setUrl(String url) {
		this.url = url;
		return (T) this;
	}

	/**
	 * Gets the conversation service.
	 *
	 * @return the conversation service
	 */

	/**
	 * @return the positiveResponses
	 */
	public List<String> getPositiveResponses() {
		return positiveResponses;
	}

	/**
	 * @param positiveResponses the positiveResponses to set
	 */
	public T setPositiveResponses(List<String> positiveResponses) {
		this.positiveResponses = positiveResponses;
		return (T) this;
	}
	
	
	
}
