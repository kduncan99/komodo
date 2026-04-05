/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;

/**
 * Represents an absolute address - this is a composite value which identifies a particular
 * StaticMainStorageProcessor, and an offset from the beginning of the storage of that processor
 * which identifies a particular word of storage.
 */
public class AbsoluteAddress {

    /**
     * Indicates a particular segment - the offset is relative to the segment.
     * For hardware-emulated MSPs, there may be only one or a few segments, and the operating system is
     * responsible for managing memory there-in.
     * For pass-through MSPs, there will be a segment for every memory allocation.  The operating system
     * is responsible for requesting and releasing segments in sizes most convenient for it.
     * Range: 0:0x7FFFFFFF (31 bits)
     */
    private int _segment;

    /**
     * A value corresponding to an offset from the start of that MSP's segment.
     * Range: 0:0x7FFFFFFF (31 bits)
     */
    private int _offset;

    /**
     * Constructor from components
     * @param segment segment of a particular MSP
     * @param offset offset from the beginning of the indicated segment
     */
    public AbsoluteAddress(
        final int segment,
        final int offset
    ) {
        _segment = segment & 0x7FFFFFFF;
        _offset = offset & 0x7FFFFFFF;
    }

    /**
     * Constructor given an absolute address layout in memory
     * @param baseArray ArraySlice containing the 2-word absolute address (possibly as a proper subset of the array)
     * @param offset offset from the start of baseArray where the absolute address is located
     */
    public AbsoluteAddress(
        final ArraySlice baseArray,
        final int offset
    ) {
        this((int)baseArray.get(offset), (int)baseArray.get(offset + 1));
    }

    /**
     * Constructor given an absolute address layout in memory - as above, but using discrete long array as source
     * @param baseArray long array
     * @param offset offset from the start of baseArray where the absolute address is located
     */
    public AbsoluteAddress(
        final long[] baseArray,
        final int offset
    ) {
        this((int)baseArray[offset], (int)baseArray[offset + 1]);
    }

    public int getSegment() { return _segment; }
    public int getOffset() { return _offset; }

    public AbsoluteAddress set(final AbsoluteAddress addr) {
        _segment = addr._segment;
        _offset = addr._offset;
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
     * Creates a new AbsoluteAddress with the same segment and the given offset added to the current offset.
     * @param offset offset to be added
     */
    public AbsoluteAddress addOffset(final int offset) {
        return new AbsoluteAddress(_segment, _offset + offset);
    }

    /**
     * Populates a two-word area with our architecturally-defined pattern for representing
     * an absolute address in main storage.
     * The format is:
     *      Word 0  Bits 0-4:  zero
     *              Bits 5-35: MSP segment
     *      Word 1  Bits 0-4:  MSP UPI
     *              Bits 5-35: Offset from start of the MSP segment
     * @param arena defines an arena of memory, possibly the storage from an MSP
     * @param offset offset from the start of the arena, where we place the 3-word ACW
     */
    public void populate(
        final ArraySlice arena,
        final int offset
    ) {
        arena.set(offset, _segment & 0x7FFFFFFF);
        arena.set(offset + 1, _offset & 0x7FFFFFFF);
    }

    /**
     * Generate a hash code based solely on the values of this object
     * @return hash code
     */
    @Override
    public int hashCode() {
        return (_segment << 8) ^ _offset;
    }

    /**
     * equals method
     * @param obj comparison object
     * @return true if this object equals the comparison object
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof AbsoluteAddress aa) && (_segment == aa._segment) && (_offset == aa._offset);
    }

    @Override
    public String toString() {
        return String.format("0%o:%012o", _segment, _offset);
    }
}
