/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.nio.ByteBuffer;

/**
 * Wraps an integer in a thin wrapper to help enhance type checking.
 * Used for sizes which fit into a signed integer.
 */
public class Size {

    private int _value;

    /**
     * Default constructor
     */
    Size() { _value = 0; }
    Size(int value) { _value = value; }

    /**
     * Deserializes a value into this object from the given ByteBuffer
     */
    public void deserialize(
        final ByteBuffer buffer
    ) {
        _value = buffer.getInt();
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof Size) && (_value == ((Size)obj)._value);
    }

    @Override
    public int hashCode() { return _value; }

    /**
     * Getter
     */
    public int getValue(
    ) {
        return _value;
    }


    /**
     * Serializes this object into the given ByteBuffer
     */
    public void serialize(
        final ByteBuffer buffer
    ) {
        buffer.putInt(_value);
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
