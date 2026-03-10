/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;

/**
 * Represents an absolute address - this is a composite value which identifies a particular
 * StaticMainStorageProcessor, and an offset from the beginning of the storage of that processor
 * which identifies a particular word of storage.
 * TODO think about this
 */
public class AbsoluteAddress {

    /**
     * The UPI (Unique Processor Index) index identifying a particular MSP
     * Range: 0:0x0F
     */
    private int _upiIndex;

    /**
     * Indicates a particular segment - the offset is relative to the segment.
     * For hardware-emulated MSPs, there may be only one or a few segments, and the operating system is
     * responsible for managing memory there-in.
     * For pass-through MSPs, there will be a segment for every memory allocation.  The operating system
     * is responsible for requesting and releasing segments in sizes most convenient for it.
     * Range: 0:0x7FFFFFF (25 bits)
     */
    private int _segment;

    /**
     * A value corresponding to an offset from the start of that MSP's segment.
     * Range: 0:0_377777_777777 (35 bits)
     */
    private int _offset;

    /**
     * Constructor from components
     * @param upiIndex UPI of an MSP
     * @param segment segment of a particular MSP
     * @param offset offset from the beginning of the indicated segment
     */
    public AbsoluteAddress(final int upiIndex,
                           final int segment,
                           final int offset) {
        _upiIndex = upiIndex & 0xF;
        _segment = segment;
        _offset = offset;
    }

    public AbsoluteAddress(final long word1,
                           final long word2) {
        _segment = (int) (word1 & 0xFFFFFFFFL);
        _upiIndex = (int) ((word2 >> 32) & 0xF);
        _offset = (int) (word2 & 0xFFFFFFFFL);
    }

    /**
     * Constructor given an absolute address layout in memory
     * @param baseArray ArraySlice containing the 2-word absolute address (possibly as a proper subset of the array)
     * @param offset offset from the start of baseArray where the absolute address is located
     */
    public AbsoluteAddress(final ArraySlice baseArray,
                           final int offset) {
        this(baseArray.get(offset), baseArray.get(offset + 1));
    }

    /**
     * Constructor given an absolute address layout in memory - as above, but using discrete long array as source
     * @param baseArray long array
     * @param offset offset from the start of baseArray where the absolute address is located
     */
    public AbsoluteAddress(final long[] baseArray,
                           final int offset) {
        this(baseArray[offset], baseArray[offset + 1]);
    }

    public int getUpiIndex() { return _upiIndex; }
    public int getSegment() { return _segment; }
    public int getOffset() { return _offset; }

    public AbsoluteAddress set(final AbsoluteAddress addr) {
        _upiIndex = addr._upiIndex;
        _segment = addr._segment;
        _offset = addr._offset;
        return this;
    }

    public AbsoluteAddress setUpiIndex(final int upiIndex) {
        _upiIndex = upiIndex;
        return this;
    }

    public AbsoluteAddress setSegment(final int segment) {
        _segment = segment;
        return this;
    }

    public AbsoluteAddress setOffset(final int offset) {
        _offset = offset;
        return this;
    }

    /**
     * Adds another offset to the offset in this object
     * @param offset offset to be added
     */
    public AbsoluteAddress addOffset(final int offset) {
        return new AbsoluteAddress(_upiIndex, _segment, _offset + offset);
    }

    /**
     * Populates a two-word area with our architecturally-defined pattern for representing
     * an absolute address in main storage.
     * The format is:
     *      Word 0  Bits 0-3:  zero
     *              Bits 4-35: MSP segment
     *      Word 1  Bits 0-3:  MSP UPI
     *      Word 1  Bits 4-35: Offset from start of the MSP segment
     * @param arena defines an arena of memory, possibly the storage from an MSP
     * @param offset offset from the start of the arena, where we place the 3-word ACW
     */
    public void populate(final ArraySlice arena,
                         final int offset) {
        long w0 = (long) _segment & 0xFFFFFFFFL;
        long w1 = ((long) (_upiIndex & 0xF) << 32) | ((long) _offset & 0xFFFFFFFFL);
        arena.set(offset, w0);
        arena.set(offset + 1, w1);
    }

    /**
     * Generate a hash code based solely on the values of this object
     * @return hash code
     */
    @Override
    public int hashCode() {
        return _upiIndex ^ _segment ^ (int)_offset;
    }

    /**
     * equals method
     * @param obj comparison object
     * @return true if this object equals the comparison object
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof AbsoluteAddress aa)
               && (_upiIndex == aa._upiIndex)
               && (_segment == aa._segment)
               && (_offset == aa._offset);
    }

    @Override
    public String toString() {
        return String.format("0%o:0%o:%012o", _upiIndex, _segment, _offset);
    }
}
