/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when a caller wants to connect two Nodes which cannot be connected as requested
 */
public class CannotConnectException extends Exception {

    public CannotConnectException(
        final String message
    ) {
        super(message);
    }
}
