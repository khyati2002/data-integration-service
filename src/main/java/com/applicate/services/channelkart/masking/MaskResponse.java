package com.applicate.services.channelkart.masking;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaskResponse {

    PropertyDefinition value();
}