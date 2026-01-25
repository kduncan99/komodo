/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public class InvalidCharacterException extends StreamException {

    public InvalidCharacterException(final byte ch) {
        super(String.format("Invalid character [0x%02X]", ch));
    }
}
