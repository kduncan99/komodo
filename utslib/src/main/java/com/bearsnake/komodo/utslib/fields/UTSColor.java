/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.fields;

public enum UTSColor {
    BLACK((byte) 0x00),
    RED((byte) 0x01),
    GREEN((byte) 0x02),
    YELLOW((byte) 0x03),
    BLUE((byte) 0x04),
    MAGENTA((byte) 0x05),
    CYAN((byte) 0x06),
    WHITE((byte) 0x07);

    private final byte _byteValue;

    UTSColor(final byte byteValue) {
        _byteValue = byteValue;
    }

    public byte getByteValue() {
        return _byteValue;
    }

    public static UTSColor fromByte(byte b) {
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
