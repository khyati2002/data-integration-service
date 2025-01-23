/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.transformers.impl;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.transformers.TransformerInfo;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class JoltTransformer.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class JoltTransformer extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

    private static Logger logger = LoggerFactory.getLogger(JoltTransformer.class);

    private TransformerInfo transformerInfo;

    @Override
    public Map<String,Object> transform(Map<String,Object> jsonobj) {
        transformerInfo= this.getTransformerInfo();
        ArrayNode code_node= transformerInfo.getCode();
        if(NullUtils.isNotNull(code_node) && NullUtils.isNotNull(jsonobj)) {
            try {
                Object spec= JsonUtils.jsonToObject(String.valueOf(code_node));
                Chainr chainr = Chainr.fromSpec(spec);
                Object transformedOutput = chainr.transform(jsonobj);
                return JSONUtils.getObjectMapper().readValue(JsonUtils.toJsonString(transformedOutput),new TypeReference<HashMap<String,Object>>(){});
            }
            catch(Exception ex) {
                logger.error("Jolt Transformer Exception",ex);
                return null;
            }
        }
        else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
    }

}
