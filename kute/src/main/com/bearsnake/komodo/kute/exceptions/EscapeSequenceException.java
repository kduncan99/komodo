/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public class EscapeSequenceException extends StreamException {

    public EscapeSequenceException(final String message) {
        super("Invalid escape sequence:" + message);
    }

    public EscapeSequenceException(final byte ch) {
        super(String.format("Invalid escape sequence ESC [0x%02X]", ch));
    }
}
