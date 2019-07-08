/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.nio.ByteBuffer;

/**
 * Wraps an integer in a thin wrapper to help enhance type checking.
 * Caller should avoid using negative numbers.  We would like to make this unsigned, but Java doens't do unsigned longs.
 */
public class Identifier {

    private long _value;

    Identifier() { _value = 0; }
    Identifier(long value) { _value = value; }

    /**
     * Deserializes a value into this object from the given ByteBuffer
     */
    public void deserialize(
        final ByteBuffer buffer
    ) {
        _value = buffer.getLong();
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof Identifier) && (_value == ((Identifier)obj)._value);
    }

    public int hashCode() { return (int) _value; }
    public long getValue() { return _value; }


    /**
     * Serializes this object into the given ByteBuffer
     */
    public void serialize(
        final ByteBuffer buffer
    ) {
        buffer.putLong(_value);
    }

    /**
     * For display/logging
     */
    @Override
    public String toString(
    ) {
        return String.format("%d", _value);
    }
}
