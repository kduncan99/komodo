/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.types;

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
    public Size(
    ) {
        _value = 0;
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public Size(
        final int value
    ) {
        _value = value;
    }

    /**
     * Deserializes a value into this object from the given ByteBuffer
     * <p>
     * @param buffer
     */
    public void deserialize(
        final ByteBuffer buffer
    ) {
        _value = buffer.getInt();
    }

    /**
     * Comparator
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj != null) && (obj instanceof Size) && (_value == ((Size)obj)._value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getValue(
    ) {
        return _value;
    }


    /**
     * Serializes this object into the given ByteBuffer
     * <p>
     * @param buffer
     */
    public void serialize(
        final ByteBuffer buffer
    ) {
        buffer.putInt(_value);
    }

    /**
     * For display/logging
     * <p>
     * @return
     */
    @Override
    public String toString(
    ) {
        return String.format("%d", _value);
    }
}
