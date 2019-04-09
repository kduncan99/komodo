/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.exceptions;

/**
 * Exception thrown by a method when some provided argument is invalid
 * This is a Throwable because we want to use it liberally to detect programming errors
 * but we don't want to have to catch it everywhere, when we expect that it should never occur once development is complete.
 */
public class InvalidArgumentRuntimeException extends RuntimeException {

    public InvalidArgumentRuntimeException(
        final String message
    ) {
        super(message);
    }
}
