/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown by a method when an invalid message identifier is detected
 */
public class InvalidMessageIdException extends Exception {

    public InvalidMessageIdException(
        final int messageId
    ) {
        super(String.format("Invalid Message Id:%d", messageId));
    }
}
