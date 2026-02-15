/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.exceptions;

public class UTSInvalidFCCSequenceException extends UTSException {

    public UTSInvalidFCCSequenceException() {
        super("Invalid FCC sequence");
    }

    public UTSInvalidFCCSequenceException(final String message) {
        super("Invalid FCC sequence:" + message);
    }
}
