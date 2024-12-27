/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.exceptions;


import com.salescode.channelkart.utils.StringUtils;

/**
 * The class TransformationException.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
public class TransformationException extends ExecutionInteruptedException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = -5591285764653259763L;

    /**
     * Instantiates a new transformation exception.
     *
     * @param message the message
     * @param values  the values
     */
    public TransformationException(String message, String... values) {
        super(StringUtils.format(message, values));
    }

    /**
     * Instantiates a new transformation exception.
     *
     * @param cause   the cause
     * @param message the message
     * @param values  the values
     */
    public TransformationException(Throwable cause, String message, String... values) {
        super(cause, StringUtils.format(message, values));
    }

    /**
     * Instantiates a new transformation exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public TransformationException(String message, Throwable cause) {
        super(cause, message);
    }

}
