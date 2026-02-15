/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

public enum PrintMode {
    PRINT("PRNT"),
    FORM("FORM"),
    TRANSPARENT("XPAR");

    private final String _string;

    PrintMode(final String str) {
        _string = str;
    }

    public static PrintMode getPrintMode(final String str) {
        for (PrintMode mode : PrintMode.values()) {
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
