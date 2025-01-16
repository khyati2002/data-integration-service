/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.notification;

import com.applicate.notification.model.NotificationPayload;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;

import java.lang.reflect.InvocationTargetException;

/**
 * A factory for creating NotificationPayload objects.
 * 
 * @author Manish Srivastava
 * @since  Mar 2021
 * @version 1.0
 */
public final class NotificationPayloadFactory {
	
	private NotificationPayloadFactory() {}

	/**
	 * Factory.
	 *
	 * @param <T> the generic type
	 * @param type the type
	 * @return the t
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends NotificationPayload> T factory(NotificationTypeRegistry type) {
		Class clazz= type.getNotificationPayload();
		try {
			return (T) clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new CustomRuntimeException(e);
		}
	}

}
