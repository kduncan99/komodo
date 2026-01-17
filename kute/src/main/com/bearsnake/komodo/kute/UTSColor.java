/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.scene.paint.Color;

public enum UTSColor {
    BLACK((byte) 0x00, Color.rgb(0, 0, 0)),
    RED((byte) 0x01, Color.rgb(255, 0, 0)),
    GREEN((byte) 0x02, Color.rgb(0, 255, 0)),
    YELLOW((byte) 0x03, Color.rgb(255, 255, 0)),
    BLUE((byte) 0x04, Color.rgb(0, 0, 255)),
    MAGENTA((byte) 0x05, Color.rgb(255, 0, 255)),
    CYAN((byte) 0x06, Color.rgb(0, 255, 255)),
    WHITE((byte) 0x07, Color.rgb(255, 255, 255));

    private final byte _byteValue;
    private final Color _fxTextColor;

    UTSColor(final byte byteValue, final Color color) {
        _byteValue = byteValue;
        _fxTextColor = color;
    }

    public byte getByteValue() {
        return _byteValue;
    }

    static UTSColor fromByte(byte b) {
        return switch(b) {
            case 0 -> BLACK;
            case 1 -> RED;
            case 2 -> GREEN;
            case 3 -> YELLOW;
            case 4 -> BLUE;
            case 5 -> MAGENTA;
            case 6 -> CYAN;
            case 7 -> WHITE;
            default -> null;
        };
    }

    public Color getFxTextColor() {
        return _fxTextColor;
    }

    public UTSColor nextColor() {
        return switch (this) {
            case BLACK -> RED;
            case RED -> GREEN;
            case GREEN -> YELLOW;
            case YELLOW -> BLUE;
            case BLUE -> MAGENTA;
            case MAGENTA -> CYAN;
            case CYAN -> WHITE;
            case WHITE -> BLACK;
        };
    }
}
