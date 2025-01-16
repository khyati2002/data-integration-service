package com.applicate.notification;

import com.applicate.notification.model.NotificationModel;
import com.applicate.notification.response.NotificationResponse;

public interface Notifiable extends NotifiableType{

	public NotificationResponse sendNotification(NotificationModel model);
	
}
