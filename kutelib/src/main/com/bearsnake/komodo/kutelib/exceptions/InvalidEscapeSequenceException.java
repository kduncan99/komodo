/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class InvalidEscapeSequenceException extends StreamException {

    public InvalidEscapeSequenceException() {
        super("Invalid escape sequence");
    }

    public InvalidEscapeSequenceException(final String message) {
        super("Invalid escape sequence:" + message);
    }

    public InvalidEscapeSequenceException(final byte ch) {
        super(String.format("Invalid escape sequence ESC [0x%02X]", ch));
    }
}
