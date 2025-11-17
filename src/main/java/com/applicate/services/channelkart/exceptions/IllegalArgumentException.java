package com.applicate.services.channelkart.exceptions;

import com.applicate.services.channelkart.utils.StringUtils;

public class IllegalArgumentException extends CustomRuntimeException {

    private static final long serialVersionUID = -5232625820041548520L;

    public IllegalArgumentException(String message,String ...values) {
        super(StringUtils.format(message, values));
    }

    public IllegalArgumentException(Throwable cause,String message,String ...values) {
        super(cause,StringUtils.format(message, values));
    }

}
