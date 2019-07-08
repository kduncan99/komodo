/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.nio.ByteBuffer;

/**
 * Wraps an integer in a thin wrapper to help enhance type checking.
 * Caller should avoid using negative numbers.  This *should* be unsigned, but that could lead to a worm can.
 */
public class Counter {

    private long _value;

    Counter() { _value = 0; }
    Counter(long value) { _value = value; }

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
        return (obj instanceof Counter) && (_value == ((Counter)obj)._value);
    }

    @Override
    public int hashCode() {
        return (int) _value;
    }

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
     * For display/logging purposes
     */
    @Override
    public String toString(
    ) {
        return String.format("%d", _value);
    }
}
