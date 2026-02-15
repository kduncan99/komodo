/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.exceptions;

public class UTSInvalidEscapeSequenceException extends UTSException {

    public UTSInvalidEscapeSequenceException() {
        super("Invalid escape sequence");
    }

    public UTSInvalidEscapeSequenceException(final String message) {
        super("Invalid escape sequence:" + message);
    }

    public UTSInvalidEscapeSequenceException(final byte ch) {
        super(String.format("Invalid escape sequence ESC [0x%02X]", ch));
    }
}
