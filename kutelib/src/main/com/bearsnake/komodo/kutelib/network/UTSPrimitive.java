/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

public enum UTSPrimitive {
    CURSOR_TO_HOME,
    ERASE_DISPLAY,
    PUT_ESCAPE;

    @Override
    public String toString() {
        return switch (this) {
            case CURSOR_TO_HOME -> "CRS_HOME";
            case ERASE_DISPLAY -> "ERS_DSP";
            case PUT_ESCAPE -> "PUT_ESC";
        };
    }
}
