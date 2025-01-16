/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.notification;

import com.applicate.notification.model.NotificationPayload;
import org.apache.commons.lang3.StringUtils;

/**
 * The enum NotificationTypeRegistry.
 *
 * @author  Manish Srivastava
 * @since   Feb 2021
 */
@SuppressWarnings("rawtypes")
public enum NotificationTypeRegistry {

	/** The sms. */
	SMS {
		@Override
		public Class getNotificationPayload() {
			return NotificationPayload.class;
		}
	}, 
	
	/** The email. */
	EMAIL {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	},
	
	/** The facebook. */
	FACEBOOK {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	},
	
	/** The whatsapp. */
	WHATSAPP {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	},
	
	/** The viber. */
	VIBER {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	},
	
	/** The line. */
	LINE {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	},
	
	/** The zalo. */
	ZALO {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	}, 
	
	/** The firebase. */
	FIREBASE {
		@Override
		public Class getNotificationPayload() {
			return null;
		}
	};
	
	public abstract Class getNotificationPayload();
	
	public static NotificationTypeRegistry getRegistry(String str) {
		if(StringUtils.isNotBlank(str)) {
			return NotificationTypeRegistry.valueOf(str.toUpperCase());
		}
		throw new IllegalArgumentException("type cannot be null or blank");
	}
	
	public boolean isEquals(NotificationTypeRegistry type) {
		if(type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		return this.name().equalsIgnoreCase(type.name());
	}

	public boolean isSameChannel(String channelName) {
		return this.name().equalsIgnoreCase(channelName);
	}
	
}
