/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.exceptions;

/**
 * Thrown when calling code requests something which doesn't exist.
 */
public class NotFoundException extends AssemblerException {

    public NotFoundException(
    ) {
        super();
    }

    public NotFoundException(
        final String message
    ) {
        super(message);
    }
}
