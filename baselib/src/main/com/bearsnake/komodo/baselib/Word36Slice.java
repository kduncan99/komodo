/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.util.Arrays;

public class Word36Slice {

    protected final long[] _baseArray;
    protected final int _offset;
    protected final int _length;

    public Word36Slice(long[] source) {
        _baseArray = source;
        _offset = 0;
        _length = source.length;
    }

    public Word36Slice(long[] source,
                       int offset,
                       int length) {
        if ((offset < 0) || (offset >= source.length)) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (length < 0) {
            throw new NegativeArraySizeException();
        } else if (offset + length >= source.length) {
            throw new ArrayIndexOutOfBoundsException(offset + length);
        }

        _baseArray = source;
        _offset = offset;
        _length = length;
    }

    public Word36Slice(Word36Slice source) {
        _baseArray = source._baseArray;
        _offset = source._offset;
        _length = source._length;
    }

    public Word36Slice(Word36Slice source,
                       int offset,
                       int length) {
        if ((offset < 0) || (offset >= source.getLength())) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (length < 0) {
            throw new NegativeArraySizeException();
        } else if (offset + length >= source.getLength()) {
            throw new ArrayIndexOutOfBoundsException(offset + length);
        }

        _baseArray = source._baseArray;
        _offset = source._offset + offset;
        _length = length;
    }

    public int getLength() {
        return _length;
    }

    public Word36Slice getSlice(int offset,
                                int length) {
        return new Word36Slice(this, offset, length);
    }

    public long get(int index) {
        if ((index < 0) || (index >= _length)) {
            throw new IndexOutOfBoundsException(index);
        }
        return _baseArray[_offset + index];
    }

    public Word36 getWord36(int index) {
        if ((index < 0) || (index >= _length)) {
            throw new IndexOutOfBoundsException(index);
        }
        return new Word36(get(index));
    }

    public long[] getArray() {
        return Arrays.copyOfRange(_baseArray, _offset, _offset + _length);
    }

    public long[] getArray(int offset,
                           int length) {
        if ((offset < 0) || (offset >= getLength())) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (length < 0) {
            throw new NegativeArraySizeException();
        } else if (offset + length >= getLength()) {
            throw new ArrayIndexOutOfBoundsException(offset + length);
        }
        return Arrays.copyOfRange(_baseArray, _offset + offset, _offset + offset + length);
    }

    public void set(int index, long value) {
        if ((index < 0) || (index >= _length)) {
            throw new IndexOutOfBoundsException(index);
        }
        _baseArray[_offset + index] = value;
    }

    public void set(int index, Word36 value) {
        if ((index < 0) || (index >= _length)) {
            throw new IndexOutOfBoundsException(index);
        }
        _baseArray[_offset + index] = value.getW();
    }
}
