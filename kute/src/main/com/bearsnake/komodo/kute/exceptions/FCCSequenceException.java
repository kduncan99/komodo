/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public class FCCSequenceException extends StreamException {

    public FCCSequenceException(final String message) {
        super("Invalid FCC sequence:" + message);
    }

    public FCCSequenceException(final String message, final byte ch2) {
        super(String.format("%s:0x%02X", message, ch2));
    }

    public FCCSequenceException(final byte ch1, final byte ch2) {
        super(String.format("Invalid FCC sequence M=0x%02X N=0x%02X", ch1, ch2));
    }
}
