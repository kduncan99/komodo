/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.util.Arrays;

public class Word36BaseSlice extends Word36Slice {

    public Word36BaseSlice(long[] source) {
        super(source);
    }

    public Word36BaseSlice(long[] source,
                           int offset,
                           int length) {
        super(source);
        if (offset != 0) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (length != source.length) {
            throw new ArrayIndexOutOfBoundsException(length);
        }
    }

    public Word36BaseSlice(Word36Slice source) {
        this(source._baseArray, source._offset, source._length);
    }

    public Word36BaseSlice(Word36Slice source,
                           int offset,
                           int length) {
        this(source._baseArray, source._offset + offset, source._length + length);
    }

    @Override
    public long get(int index) {
        return _baseArray[_offset + index];
    }

    @Override
    public Word36 getWord36(int index) {
        return new Word36(get(index));
    }

    @Override
    public long[] getArray() {
        return Arrays.copyOf(_baseArray, _baseArray.length);
    }

    @Override
    public long[] getArray(int offset,
                           int length) {
        return Arrays.copyOfRange(_baseArray, offset, offset + length);
    }

    @Override
    public void set(int index, long value) {
        _baseArray[index] = value;
    }

    @Override
    public void set(int index, Word36 value) {
        _baseArray[index] = value.getW();
    }
}
