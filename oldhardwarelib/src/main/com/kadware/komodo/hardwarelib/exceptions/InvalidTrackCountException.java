/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown by a method when an invalid track count is detected
 */
public class InvalidTrackCountException extends Exception {

    public InvalidTrackCountException(
        final long trackCount
    ) {
        super(String.format("Invalid Track Count:%d", trackCount));
    }
}
