/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class FunctionKeyException
    extends KuteException {

    public FunctionKeyException(final String message) {
        super(message);
    }

    public FunctionKeyException(final int fkey) {
        super(String.format("Invalid function key %d", fkey));
    }
}
