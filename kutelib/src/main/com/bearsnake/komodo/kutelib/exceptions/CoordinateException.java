/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class CoordinateException extends KuteException {

    public CoordinateException(final String message) {
        super(message);
    }

    public CoordinateException(final int coordinate) {
        super(String.format("Invalid coordinate %d", coordinate));
    }
}
