package com.applicate.services.channelkart.exceptions;

import com.applicate.services.channelkart.utils.StringUtils;

/**
 * The class IllegalArgumentException.
 *
 * @author Manish Srivastava
 * @since  Oct 2020
 */
public class IllegalArgumentException extends CustomRuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -5232625820041548520L;

    /**
     * Instantiates a new illegal argument exception.
     *
     * @param message the message
     * @param values the values
     */
    public IllegalArgumentException(String message,String ...values) {
        super(StringUtils.format(message, values));
    }

    /**
     * Instantiates a new illegal argument exception.
     *
     * @param cause the cause
     * @param message the message
     * @param values the values
     */
    public IllegalArgumentException(Throwable cause,String message,String ...values) {
        super(cause,StringUtils.format(message, values));
    }

}
