package com.applicate.services.channelkart.templates;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author : Jinu
 * Date    : 6/26/2020
 **/
public enum ReferenceType {

   INLINE, URL;

   @JsonCreator
   public static ReferenceType parse(String templateType) {
      return valueOf(templateType.toUpperCase());
   }
}
