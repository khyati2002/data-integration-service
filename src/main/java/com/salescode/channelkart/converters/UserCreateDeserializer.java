/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.converters;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.services.UserService;
import com.salescode.channelkart.utils.JSONUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * The Class UserCreateDeserializer.
 * 
 * @author Manish Srivastava
 * @since  Apr 2020
 * @version 1.1
 */
public class UserCreateDeserializer extends JsonDeserializer<User> {

	/**
	 * Convert.
	 *
	 * @param value the value
	 * @return the user
	 */
	public User convert(String value) {
		if(value != null) {
			UserService service=(UserService) ServiceLocator.lookup(User.class);
			User user=service.findByLoginId(value,false);
			if(user==null){
				user= new User();
				user.setUserAccountId(value);
				user.setLoginId(value);
				user.setMobile("0000000000");
				user.setPassword(UUID.randomUUID().toString());
				user.setName(value);
				user.setActiveStatus(ActiveStatus.ACTIVE);
			}
			return user;
		}
		return null;
	}

	/**
	 * Deserialize.
	 *
	 * @param jsonParser the json parser
	 * @param ctxt the ctxt
	 * @return the user
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JsonProcessingException the json processing exception
	 */
	@Override
	public User deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        if(node != null && !node.isNull()) {
        	if(node.isTextual()) {
        		return convert(node.asText());
        	}else {
        		return JSONUtils.convert(node, User.class);
        	}
        }
        return null;
	}

}


