/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

import com.kadware.komodo.baselib.TrackCount;

/**
 * Exception thrown by a method when an invalid track count is detected
 */
public class InvalidTrackCountException extends Exception {

    public InvalidTrackCountException(
        final TrackCount trackCount
    ) {
        super(String.format("Invalid Track Count:%s", String.valueOf(trackCount)));
    }
}
