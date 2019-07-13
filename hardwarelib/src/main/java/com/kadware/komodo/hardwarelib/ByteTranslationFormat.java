/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Represents the various methods of translating byte streams to word buffers
 */
@SuppressWarnings("Duplicates")
public enum ByteTranslationFormat {
    QuarterWordPerByte(0),                  //  Format A
    SixthWordByte(1),                       //  Format B
    QuarterWordPerPacked(2),                //  Format C
    QuarterWordPerByteNoTermination(3);     //  Format D

    private final int _code;

    ByteTranslationFormat(int code) { _code = code; }

    public int getCode() { return _code; }

    public static ByteTranslationFormat getValue(
        final int code
    ) {
        switch (code) {
            case 0:     return QuarterWordPerByte;
            case 1:     return SixthWordByte;
            case 2:     return QuarterWordPerPacked;
            case 3:     return QuarterWordPerByteNoTermination;
            default:    return null;
        }
    }
}
