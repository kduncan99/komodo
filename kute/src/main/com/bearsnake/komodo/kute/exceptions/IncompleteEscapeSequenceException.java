/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public class IncompleteEscapeSequenceException extends EscapeSequenceException {

    public IncompleteEscapeSequenceException() {
        super("Incomplete Escape Sequence");
    }
}
