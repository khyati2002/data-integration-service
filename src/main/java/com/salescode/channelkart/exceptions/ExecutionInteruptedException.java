/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.exceptions;

import com.salescode.channelkart.utils.StringUtils;

/**
 * The class ExecutionInteruptedException.
 * <p>
 * Extend this exception in case of interrupting any execution service.
 *
 * @author Manish Srivastava
 * @see com.applicate.services.channelkart.taskexecutors.MDMUploader
 * @since Oct 2020
 */
public class ExecutionInteruptedException extends CustomRuntimeException {

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    public ExecutionInteruptedException(String message, String... values) {
        super(StringUtils.format(message, values));
    }

    public ExecutionInteruptedException(Throwable cause, String message, String... values) {
        super(cause, StringUtils.format(message, values));
    }

}
