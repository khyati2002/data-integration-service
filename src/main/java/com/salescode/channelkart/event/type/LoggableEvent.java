package com.salescode.channelkart.event.type;

import java.util.Map;

public interface LoggableEvent {

    Map<String, String> getLoggingAttributes();
}
