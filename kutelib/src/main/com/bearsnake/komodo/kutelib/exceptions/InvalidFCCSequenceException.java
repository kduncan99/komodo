/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class InvalidFCCSequenceException
    extends StreamException {

    public InvalidFCCSequenceException() {
        super("Invalid FCC sequence");
    }

    public InvalidFCCSequenceException(final String message) {
        super("Invalid FCC sequence:" + message);
    }
}
