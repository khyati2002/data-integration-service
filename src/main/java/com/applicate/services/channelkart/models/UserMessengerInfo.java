/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.services.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * The class UserMessengerInfo.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@Embeddable
public final class UserMessengerInfo implements Serializable{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -2711319085110590315L;

	/** The channel. */
	@NotNull
	@NotBlank
	private String channel;
	
	/** The channel id. */
	@NotBlank
	@Column(unique=true)
	private String channelId;

	/**
	 * @return the channel
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * @param channel the channel to set
	 */
	public UserMessengerInfo setChannel(String channel) {
		this.channel = channel;
		return this;
	}

	/**
	 * @return the channelId
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * @param channelId the channelId to set
	 */
	public UserMessengerInfo setChannelId(String channelId) {
		this.channelId = channelId;
		return this;
	}
	
	@Transient
	@JsonIgnore
	public static UserMessengerInfo Builder() {
		return new UserMessengerInfo();
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "UserMessengerInfo [channel=" + channel + ", channelId=" + channelId + "]";
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return ((channelId == null) ? 0 : channelId.hashCode());
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserMessengerInfo other = (UserMessengerInfo) obj;
		if (channelId == null) {
			if (other.channelId != null)
				return false;
		} else if (!channelId.equals(other.channelId))
			return false;
		return true;
	}

}
