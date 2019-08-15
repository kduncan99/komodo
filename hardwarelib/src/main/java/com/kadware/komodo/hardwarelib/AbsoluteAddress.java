/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;

/**
* Represents an absolute address - this is a composite value which identifies a particular
* StaticMainStorageProcessor, and an offset from the beginning of the storage of that processor
* which identifies a particular word of storage.
*/
public class AbsoluteAddress {

    /**
     * The UPI (Unique Processor Index) index identifying a particular MSP
     * Range: 0:0x0F
     */
    public final int _upiIndex;

    /**
     * Indicates a particular segment - the offset is relative to the segment.
     * For hardware-emulated MSPs, there may be only one or a few segments, and the operating system is
     * responsible for managing memory there-in.
     * For pass-through MSPs, there will be a segment for every memory allocation.  The operating system
     * is responsible for requesting and releasing segments in sizes most convenient for it.
     * Range: 0:0x7FFFFFF (25 bits)
     */
    public final int _segment;

    /**
     * A value corresponding to an offset from the start of that MSP's segment.
     * Range: 0:0_377777_777777 (35 bits)
     */
    public final int _offset;

    /**
     * Constructor from components
     * @param upiIndex UPI of an MSP
     * @param segment segment of a particular MSP
     * @param offset offset from the beginning of the indicated segment
     */
    public AbsoluteAddress(
        final int upiIndex,
        final int segment,
        final int offset
    ) {
        assert((upiIndex >= 0) && (upiIndex < 16));
        assert((segment & 0xFE000000) == 0);
        assert(offset >= 0);
        _upiIndex = upiIndex;
        _segment = segment;
        _offset = offset;
    }

    /**
     * Constructor given an absolute address layout in memory
     * @param baseArray ArraySlice containing the 2-word absolute address
     * @param offset offset from the start of baseArray where the absolute address is located
     */
    public AbsoluteAddress(
        final ArraySlice baseArray,
        final int offset
    ) {
        long w1 = baseArray.get(offset);
        long w2 = baseArray.get(offset + 1);
        _segment = (int) (w1 & 0x1FFFFFF);
        _upiIndex = (int) (w2 >> 32);
        _offset = (int) w2;
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
        long w1 = baseArray[offset];
        long w2 = baseArray[offset + 1];
        _segment = (int) (w1 & 0x1FFFFFF);
        _upiIndex = (int) (w2 >> 32);
        _offset = (int) w2;
    }

    /**
     * Adds another offset to the offset in this object
     * @param offset offset to be added
     */
    public AbsoluteAddress addOffset(
        final int offset
    ) {
        return new AbsoluteAddress(_upiIndex, _segment, _offset + offset);
    }

    /**
     * Generate a hash code based solely on the values of this object
     * @return hash code
     */
    @Override
    public int hashCode(
    ) {
        return (new Integer(_upiIndex)).hashCode() ^ (new Integer(_segment)).hashCode() ^ (new Integer((int)_offset)).hashCode();
    }

    /**
     * equals method
     * @param obj comparison object
     * @return true if this object equals the comparison object
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof AbsoluteAddress) {
            AbsoluteAddress comp = (AbsoluteAddress) obj;
            return (_upiIndex == comp._upiIndex) && (_segment == comp._segment) && (_offset == comp._offset);
        } else {
            return false;
        }
    }

    @Override
    public String toString(
    ) {
        return String.format("0%o:0%o:%012o", _upiIndex, _segment, _offset);
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
    public void populate(
        final ArraySlice arena,
        final int offset
    ) {
        arena.set(offset, _segment);
        arena.set(offset + 1, ((long)(_upiIndex) << 32) | _offset);
    }
}
