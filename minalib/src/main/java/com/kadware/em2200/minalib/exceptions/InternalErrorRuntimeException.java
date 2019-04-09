/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.exceptions;

/**
 * Used when something went badly wrong - we don't usually catch these
 */
public class InternalErrorRuntimeException extends RuntimeException {

    public InternalErrorRuntimeException(
        final String message
    ) {
        super(message);
    }
}
