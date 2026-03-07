/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.primitives;

import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.exceptions.UTSCoordinateException;

public class PutCharacterDecimalPrimitive extends Primitive {

    private final byte _value;

    public PutCharacterDecimalPrimitive(final byte value) {
        super(PrimitiveType.CURSOR_POSITION);
        _value = (byte)(value & 0x7F);
    }

    public byte getValue() { return _value; }

    @Override
    public void serialize(final UTSByteBuffer destination) throws UTSCoordinateException {
        destination.putString(toString());
    }

    @Override
    public String toString() {
        return String.format("{%d}", _value);
    }
}
