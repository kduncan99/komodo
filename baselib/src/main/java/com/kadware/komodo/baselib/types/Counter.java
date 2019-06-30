/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.types;

import java.nio.ByteBuffer;

/**
 * Wraps an integer in a thin wrapper to help enhance type checking.
 * Caller should avoid using negative numbers.  We would like to make this unsigned, but Java doens't do unsigned longs.
 */
public class Counter {

    private long _value;

    /**
     * Default constructor
     */
    public Counter(
    ) {
        _value = 0;
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public Counter(
        final long value
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
        _value = buffer.getLong();
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
        return (obj != null) && (obj instanceof Counter) && (_value == ((Counter)obj)._value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getValue(
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
        buffer.putLong(_value);
    }

    /**
     * For display/logging purposes
     * <p>
     * @return
     */
    @Override
    public String toString(
    ) {
        return String.format("%d", _value);
    }
}
