/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

public enum TransmitMode {
    ALL("ALL"),
    VARIABLE("VAR"),
    CHANGED("CHAN");

    private final String _string;

    TransmitMode(final String str) {
        _string = str;
    }

    public static TransmitMode getTransmitMode(final String str) {
        for (TransmitMode mode : TransmitMode.values()) {
            if (mode._string.equals(str)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return _string;
    }
}
