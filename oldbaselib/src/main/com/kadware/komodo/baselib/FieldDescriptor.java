/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * General-purpose class describing some portion of a 36-bit word
 */
public class FieldDescriptor {

    public final int _startingBit;      //  0 is MSB, 35 is LSB
    public final int _fieldSize;        //  number of bits in the field

    public static final FieldDescriptor H1 = new FieldDescriptor(0, 18);
    public static final FieldDescriptor H2 = new FieldDescriptor(18, 18);
    public static final FieldDescriptor Q1 = new FieldDescriptor(0, 9);
    public static final FieldDescriptor Q2 = new FieldDescriptor(9, 9);
    public static final FieldDescriptor Q3 = new FieldDescriptor(18, 9);
    public static final FieldDescriptor Q4 = new FieldDescriptor(27, 9);
    public static final FieldDescriptor S1 = new FieldDescriptor(0, 6);
    public static final FieldDescriptor S2 = new FieldDescriptor(6, 6);
    public static final FieldDescriptor S3 = new FieldDescriptor(12, 6);
    public static final FieldDescriptor S4 = new FieldDescriptor(18, 6);
    public static final FieldDescriptor S5 = new FieldDescriptor(24, 6);
    public static final FieldDescriptor S6 = new FieldDescriptor(30, 6);
    public static final FieldDescriptor T1 = new FieldDescriptor(0, 12);
    public static final FieldDescriptor T2 = new FieldDescriptor(12, 12);
    public static final FieldDescriptor T3 = new FieldDescriptor(24, 12);
    public static final FieldDescriptor W = new FieldDescriptor(0, 36);

    public FieldDescriptor(
        final int startingBit,
        final int fieldSize
    ) {
        if ((startingBit < 0) || (startingBit > 35)) {
            throw new RuntimeException("Bad starting bit value: " + String.valueOf(startingBit));
        }

        if ((fieldSize < 1) || (fieldSize > 36)) {
            throw new RuntimeException("Bad field size value: " + String.valueOf(fieldSize));
        }

        if ((startingBit + fieldSize) > 36) {
            throw new RuntimeException("Bad starting bit/field size combination: "
                                       + String.valueOf(startingBit)
                                       + "/"
                                       + String.valueOf(fieldSize));
        }

        _startingBit = startingBit;
        _fieldSize = fieldSize;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof FieldDescriptor) {
            FieldDescriptor fdObj = (FieldDescriptor) obj;
            return ((fdObj._fieldSize == _fieldSize) && (fdObj._startingBit == _startingBit));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return _startingBit;
    }

    @Override
    public String toString(
    ) {
        return String.format("[%d:%d]", _startingBit, _startingBit + _fieldSize - 1);
    }
}
