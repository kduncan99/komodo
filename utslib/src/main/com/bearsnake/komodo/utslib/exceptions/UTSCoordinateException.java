/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.exceptions;

public class UTSCoordinateException extends UTSException {

    public UTSCoordinateException(final String message) {
        super(message);
    }

    public UTSCoordinateException(final int coordinate) {
        super(String.format("Invalid coordinate %d", coordinate));
    }
}
