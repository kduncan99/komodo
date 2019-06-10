/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

import com.kadware.em2200.baselib.exceptions.*;

/**
 * Extends Word36Array such that we can present an object which represnts a subset of some other underlying array
 */
public class Word36ArraySlice extends Word36Array {

    private final int _offset;
    private final int _size;

    /**
     * Constructors
     * offset and size are adjusted if and as necessary to avoid range errors
     * @param base underlying Word36Array to which we present a view
     * @param offset offset from the start of the base array, which defines the start of this view of the array
     * @param size size of this view of the array
     */
    public Word36ArraySlice(
        final Word36Array base,
        final int offset,
        final int size
    ) {
        super(base._array);

        if ((offset < 0) || (offset > base.getArraySize())) {
            throw new InvalidArgumentRuntimeException(String.format("offset is out of range of the base array:%d", offset));
        }

        if (offset + size > base.getArraySize()) {
            throw new InvalidArgumentRuntimeException(String.format("size exceeds range of the base array after offset:%d", size));
        }

        _offset = offset + base.getOffset();
        _size = size;
    }

    /**
     * Special case - pass through constructor for creating an array of a particular size.
     * In this case, this subset has a view of the entire underlying array.
     * This is for BankDescriptor, which may be constructed with a specific size as a standalone array,
     * or may be constructed as a subset of a larger storage structure.
     * @param size size to be created
     */
    public Word36ArraySlice(
        final int size
    ) {
        super(size);
        _offset = 0;
        _size = size;
    }

    /**
     * Retrieves the size of the view of the aray
     *     DO NOT REMOVE - IT IS AN OVERRIDE
     * @return value
     */
    @Override
    public int getArraySize(
    ) {
        return _size;
    }

    /**
     * Indicates the offset of this array's view of the base array.
     */
    public int getOffset() { return _offset; }

    /**
     * Retrieves the 36-bit value indicated by the index from this view of the base Array.
     *     DO NOT REMOVE - IT IS AN OVERRIDE
     * @param index index of interest
     * @return value
     */
    @Override
    public long getValue(
        final int index
    ) {
        if (index > _size) {
            throw new InvalidArgumentRuntimeException(String.format("index is out of range:%d", index));
        }
        return _array[index + _offset];
    }

    /**
     * Retrieves a Word36 object representing a particular value within this view of the base Array
     * <p>
     *     DO NOT REMOVE - IT IS AN OVERRIDE
     * </p>
     * @param index index of interest
     * @return value
     */
    @Override
    public Word36 getWord36(
        final int index
    ) {
        if (index > _size) {
            throw new InvalidArgumentRuntimeException(String.format("index is out of range:%d", index));
        }
        return new Word36(_array[index + _offset]);
    }

    /**
     * Sets the value of a particular item in this view of the base array
     *     DO NOT REMOVE - IT IS AN OVERRIDE
     * @param index index of interest
     * @param value value
     */
    @Override
    public void setValue(
        final int index,
        final long value
    ) {
        if (index > _size) {
            throw new InvalidArgumentRuntimeException(String.format("index is out of range:%d", index));
        }
        _array[index + _offset] = value & OnesComplement.BIT_MASK_36;
    }

    /**
     * Sets the value of a particular item in this view of the base array
     *     DO NOT REMOVE - IT IS AN OVERRIDE
     * @param index index of interest
     * @param value value
     */
    @Override
    public void setWord36(
        final int index,
        final Word36 value
    ) {
        if (index > _size) {
            throw new InvalidArgumentRuntimeException(String.format("index is out of range:%d", index));
        }
        _array[index + _offset] = value.getW();
    }

    public void show() {
        System.out.println(String.format("Word36ArraySlice offset=%d size=%d", _offset, _size));
        for (int sx = 0; sx < _size; ++sx) {
            System.out.println(String.format("  %4d: %012o", sx, getWord36(sx).getW()));
        }
    }
}
