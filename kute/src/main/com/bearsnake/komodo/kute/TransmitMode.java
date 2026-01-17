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

    @Override
    public String toString() {
        return _string;
    }
}
