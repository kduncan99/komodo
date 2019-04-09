/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.exceptions;

/**
 * Exception thrown by a method when it is asked to do a lookup given a partial or full key, and the lookup fails.
 */
public class NotFoundException extends Exception {

    public NotFoundException(
        final String key
    ) {
        super(String.format("Cannot find '%s'", key));
    }
}
