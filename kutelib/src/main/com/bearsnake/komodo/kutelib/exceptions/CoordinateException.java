/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class CoordinateException extends StreamException {

    public CoordinateException(final String message) {
        super(message);
    }

    public CoordinateException(final byte ch1, final byte ch2) {
        super(String.format("Invalid coordinate in stream [0x%02X][0x%02X]", ch1, ch2));
    }

    public CoordinateException(final byte ch) {
        super(String.format("Invalid coordinate in stream [0x%02X]", ch));
    }
}
