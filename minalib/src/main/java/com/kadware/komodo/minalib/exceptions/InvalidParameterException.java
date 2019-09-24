/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.exceptions;

/**
 * Thrown when calling code provides a parameter which has a value which is out of range
 */
public class InvalidParameterException extends AssemblerException {

    public InvalidParameterException(
        final String message
    ) {
        super(message);
    }
}
