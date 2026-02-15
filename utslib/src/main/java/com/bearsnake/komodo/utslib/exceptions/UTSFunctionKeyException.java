/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.exceptions;

public class UTSFunctionKeyException extends UTSException {

    public UTSFunctionKeyException(final String message) {
        super(message);
    }

    public UTSFunctionKeyException(final int fkey) {
        super(String.format("Invalid function key %d", fkey));
    }
}
