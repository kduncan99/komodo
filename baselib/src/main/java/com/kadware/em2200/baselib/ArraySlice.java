/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import com.kadware.em2200.baselib.exceptions.InvalidArgumentRuntimeException;

/**
 * Represents a subset of a base array of elements of type T
 */
public class ArraySlice {

    private final long[] _array;
    private final int _length;
    private final int _offset;

    /**
     * Constructor to produce a slice of a full array
     * @param array base array
     */
    public ArraySlice(
        final long[] array
    ) {
        _array = array;
        _offset = 0;
        _length = array.length;
    }

    /**
     * Constructor to produce a slice representing a subset of a full array
     * @param array base array
     * @param offset offset into the base array at which point this subset begins
     * @param length length of this subset
     * @throws InvalidArgumentRuntimeException if offset or index (or the combination thereof) is invalid
     */
    public ArraySlice(
        final long[] array,
        final int offset,
        final int length
    ) throws InvalidArgumentRuntimeException {
        if ((offset + length > array.length) || (offset < 0) || (length < 0)) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid arguments array size=%d requested offset=%d length=%d",
                              array.length,
                              offset,
                              length));
        }

        _array = array;
        _offset = offset;
        _length = length;
    }

    /**
     * Constructor to produce a slice representing a subset of another slice.
     * @param baseSlice base slice
     * @param offset offset into the slice's subset of the base array, at which this slice's subset begins
     * @param length length of this subset
     * @throws InvalidArgumentRuntimeException if offset or index (or the combination thereof) is invalid
     */
    public ArraySlice(
        final ArraySlice baseSlice,
        final int offset,
        final int length
    ) throws InvalidArgumentRuntimeException {
        if ((offset + length > baseSlice._length) || (offset < 0) || (length < 0)) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid arguments base slice size=%d requested offset=%d length=%d",
                              baseSlice._length,
                              offset,
                              length));
        }

        _array = baseSlice._array;
        _offset = offset + baseSlice._offset;
        _length = length;
    }

    /**
     * Gets the value at the given index
     * @param index of the value
     * @return the value
     * @throws InvalidArgumentRuntimeException if index is invalid
     */
    public long get(
        final int index
    ) throws InvalidArgumentRuntimeException {
        if ((index < 0) || (index >= _length)){
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid index=%d slice length=%d",
                              index,
                              _length));
        }

        return _array[index + _offset];
    }

    /**
     * Create a new array representing (but not backed by) the values of this subset
     * @return new array
     */
    public long[] getAll() {
        long[] result = new long[_length];
        for (int ax = _offset, rx = 0; rx < _length; ++ax, ++rx) {
            result[rx] = _array[ax];
        }
        return result;
    }

    /**
     * Loads values from the source array to the end of the source array, or to the end of this subset of the base array,
     * whichever comes first.
     * @param source source array
     */
    public void load(
        final long[] source
    ) {
        int slimit = source.length > _length ? _length : source.length;
        for (int sx = 0, ax = _offset; sx < slimit; ++sx, ++ax) {
            _array[ax] = source[sx];
        }
    }

    /**
     * Loads values from a subset of a source array into this slice at the indicated index into the slice
     * @param source source array
     * @param sourceIndex index into source array of first value to be loaded
     * @param sourceLength number of values to be loaded
     * @param destinationIndex index into destination slice of first value to be stored
     * @throws InvalidArgumentRuntimeException
     */
    public void load(
        final long[] source,
        final int sourceIndex,
        final int sourceLength,
        final int destinationIndex
    ) throws InvalidArgumentRuntimeException {
        if (sourceIndex + sourceLength > source.length) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid parameter source array length:%d source index:%d source length:%d",
                              source.length,
                              sourceIndex,
                              sourceLength));
        }

        if (destinationIndex + sourceLength > _length) {
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid parameter slice length:%d destination index:%d source length:%d",
                              _length,
                              destinationIndex,
                              sourceLength));
        }

        int slimit = sourceIndex + sourceLength;
        for (int sx = sourceIndex, ax = _offset + destinationIndex; sx < slimit; ++sx, ++ax) {
            _array[ax] = source[sx];
        }
    }

    /**
     * Sets a value into the array at the given index, which is offset further by this subset's offset
     * @param index index into the subset at which the value should be stored
     * @param value value to be stored
     * @throws InvalidArgumentRuntimeException if the index is invalid
     */
    public void set(
        final int index,
        final long value
    ) throws InvalidArgumentRuntimeException {
        if ((index < 0) || (index >= _length)){
            throw new InvalidArgumentRuntimeException(
                String.format("Invalid index=%d slice length=%d",
                              index,
                              _length));
        }

        _array[index + _offset] = value;
    }
}
