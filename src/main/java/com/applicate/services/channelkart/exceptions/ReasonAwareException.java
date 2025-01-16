package com.applicate.services.channelkart.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : Jinu
 * Date    : 7/15/2021
 **/
public interface ReasonAwareException {

   Logger log = LoggerFactory.getLogger(ReasonAwareException.class);

   String getReason();

   default boolean hasStackTraceEnabled() {
      return false;
   }
   String getErrorCode();

}
