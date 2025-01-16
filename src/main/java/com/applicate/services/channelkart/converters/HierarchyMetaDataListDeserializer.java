package com.applicate.services.channelkart.converters;


import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.utils.JSONUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HierarchyMetaDataListDeserializer extends JsonDeserializer<List<HierarchyMetaData>> {

    /**
     * this method will decide weather its string location hierarchy OR location json object
     * based on that it will convert to Location
     * @param jsonParser
     * @param ctxt
     * @return Location Object
     * @throws IOException
     */
	@Override
    public List<HierarchyMetaData> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        if(node != null && !node.isNull()) {
            if(node.isTextual()) {
            	StringToHierarchyMetaDataConverter converter= new StringToHierarchyMetaDataConverter();
                return converter.convert(node.asText());
            }else {
                return JSONUtils.getObjectMapper().readValue(JsonUtils.toPrettyJsonString(node),new TypeReference<ArrayList<HierarchyMetaData>>(){});
            }
        }
        return List.of();
    }
}