package com.salescode.channelkart.logging;

import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class RemoteMetricStream {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ArrayBlockingQueue<EventMetrics> eventQueue = new ArrayBlockingQueue<>(5000);
    public void sendAsyncEvent(EventMetrics rm){
        boolean offered = eventQueue.offer(rm);
        if(!offered){
            String errorMessage = "Failed to push message due to the event queue overflow".concat(getLob());
            logger.error(errorMessage);
        }
    }
    private String getLob(){
        return SecurityContextUtils.getLob()==null?"":" lob : ".concat(SecurityContextUtils.getLob());
    }
}
