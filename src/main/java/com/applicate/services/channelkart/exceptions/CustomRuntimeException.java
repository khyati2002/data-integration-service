package com.applicate.services.channelkart.exceptions;

import com.applicate.services.channelkart.utils.StringUtils;

public class CustomRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 4825854136668962900L;

    public CustomRuntimeException(String message,String ...values) {
        super(StringUtils.format(message, values));
    }

    public CustomRuntimeException(Throwable cause, String message,String ...values) {
        super(StringUtils.format(message, values), cause);
    }

    public String getRootCause() {
        Throwable cause = null;
        Throwable result = this;
        while(null != (cause = result.getCause())  && (result != cause) ) {
            result = cause;
        }
        return result.getMessage();
    }

    public CustomRuntimeException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
