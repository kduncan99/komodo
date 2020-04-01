/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.exceptions;

/**
 * Exception thrown by a method when it is asked to do a lookup given a partial or full key, and the lookup fails.
 * Also used for parsing, when a particular type of something is asked for but doesn't exist at the parse point.
 */
public class NotFoundException extends Exception {

    public NotFoundException(){}

    public NotFoundException(
        final String key
    ) {
        super(String.format("Cannot find '%s'", key));
    }
}
