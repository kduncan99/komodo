/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class InternalException extends RuntimeException {

    public InternalException(String message) {
        super("Internal Error:" + message);
    }
}
