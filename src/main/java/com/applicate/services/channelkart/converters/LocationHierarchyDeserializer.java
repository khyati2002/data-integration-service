/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.converters;

import com.applicate.services.channelkart.models.Location;
import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 *  The desrializer for converting json object to string location hierarchy
 *  
 *  @author Manish Srivastava
 *  @since  Nov 2021
 */
public class LocationHierarchyDeserializer extends JsonDeserializer<String> {

	/** The location service. */
	private LocationService locationService= (LocationService)ServiceLocator.lookup(Location.class);
	
    /**
     * this method will decide weather its string location hierarchy OR location json object
     * based on that it will convert to string location hierarchy
     * @param jsonParser
     * @param ctxt
     * @return Location Object
     * @throws IOException
     */
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        if(node != null && !node.isNull()) {
            if(node.isTextual()) {
                return node.asText();
            }else {
            	Location location= JSONUtils.convert(node, Location.class);
            	return locationService.formHierarchyUsingColumns(location);
            }
        }
        return null;
    }
}


