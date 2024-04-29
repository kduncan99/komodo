/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public enum TapeDensity {
    D_6250BPI(3),
    D_5090BPmm(4), // by convention, HIS98
    D_76000BPI(6), // by convention, U47M, U5136 or U5236
    D_85937BPI(7), // by convention, U7000
    D_10180BPmm(8), // by convention, U9940B
    D_MEDIUM(9), // U4980C
    D_HIGH(10), // U4980D
    D_LOW(11), // UT10K
    D_LTO(0);

    private final int _value;

    TapeDensity(final int value) {
        _value = value;
    }

    public static TapeDensity getTapeDensity(final long value) {
        for (var v : TapeDensity.values()) {
            if (v._value == value) {
                return v;
            }
        }
        return null;
    }

    public int getValue() {
        return _value;
    }
}
