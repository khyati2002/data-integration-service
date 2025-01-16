package com.applicate.notification;

import com.applicate.services.channelkart.models.UserMessengerInfo;

import java.util.List;

public interface OptInHandler {

    List<UserMessengerInfo> forceOptIn(String loginId);
    
    List<UserMessengerInfo> forceOptInForMobile(String mobileNumber);

}
