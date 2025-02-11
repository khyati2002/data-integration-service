package com.salescode.channelkart.client.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : Jinu
 * Date    : 8/19/2021
 **/
public interface RefreshableRegistry {

    Logger log = LoggerFactory.getLogger(RefreshableRegistry.class);

    void clearRegistry();
}
