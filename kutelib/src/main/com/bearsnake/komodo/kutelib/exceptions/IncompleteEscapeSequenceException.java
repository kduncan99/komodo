/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class IncompleteEscapeSequenceException extends StreamException {

    public IncompleteEscapeSequenceException() {
        super("Incomplete Escape Sequence");
    }
}
