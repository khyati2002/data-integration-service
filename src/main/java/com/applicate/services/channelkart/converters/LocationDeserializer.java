package com.applicate.services.channelkart.converters;

import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.jooq.impl.Location;

import java.io.IOException;

/**
 *  this class is for converting location hierarchy OR Location json to Location object
 */
public class LocationDeserializer extends JsonDeserializer<Location> {
    /**
     * @param value is location hierarchy
     * @return Location object from db OR null if not present
     */
    public Location convert(String value) {
        if(value != null) {
            LocationService service=(LocationService) ServiceLocator.lookup(Location.class);
            return service.findByLocationHierarchy(value);
        }
        return null;
    }

    /**
     * this method will decide weather its string location hierarchy OR location json object
     * based on that it will convert to Location
     * @param jsonParser
     * @param ctxt
     * @return Location Object
     * @throws IOException
     */
    @Override
    public Location deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        if(node != null && !node.isNull()) {
            if(node.isTextual()) {
                return convert(node.asText());
            }else {
                return JSONUtils.convert(node, Location.class);
            }
        }
        return null;
    }
}


