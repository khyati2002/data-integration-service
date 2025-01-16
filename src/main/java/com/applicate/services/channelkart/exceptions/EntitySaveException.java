package com.applicate.services.channelkart.exceptions;

import com.applicate.services.channelkart.utils.StringUtils;

public class EntitySaveException extends CustomRuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public EntitySaveException(String message,String ...values) {
		super(StringUtils.format(message, values));
	}

	public EntitySaveException(Throwable cause,String message,String ...values) {
		 super(cause, StringUtils.format(message, values));
	}
	
}
