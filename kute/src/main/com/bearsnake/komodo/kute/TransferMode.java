/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

public enum TransferMode {
    ALL("ALL"),
    VARIABLE("VAR"),
    CHANGED("CHAN");

    private final String _string;

    TransferMode(final String str) {
        _string = str;
    }

    public static TransferMode getTransferMode(final String str) {
        for (TransferMode mode : TransferMode.values()) {
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
