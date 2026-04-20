/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Represents an absolute address - this is a composite value that identifies a particular
 * StaticMainStorageProcessor, and an offset from the beginning of the storage of that processor
 * which identifies a particular word of storage.
 * It is the concatenation of two 32-bit words and is stored in main storage as a singular 64-bit integer.
 * The segment is in the MSW, and the offset is in the LSW.
 * The segment:
 *      Indicates a particular segment - the offset is relative to the segment.
 *      For hardware-emulated MSPs, there may be only one or a few segments, and the operating system is
 *      responsible for managing memory there-in.
 *      For pass-through MSPs, there will be a segment for every memory allocation.  The operating system
 *      is responsible for requesting and releasing segments in sizes most convenient for it.
 *      Range: 0:0x7FFFFFFF (31 bits)
 *  The offset:
 *      A value corresponding to an offset from the start of that MSP's segment.
 *      Range: 0:0x7FFFFFFF (31 bits)
 * We use a single 64-bit value in order to avoid leaving a lot of small objects lying about in storage.
 */
public class AbsoluteAddress {

    // This is a static class - no instances allowed
    private AbsoluteAddress() {}

    public static long construct(
        final int segment,
        final int offset
    ) {
        return (((long)(segment & 0x7FFFFFFF)) << 32) | (offset & 0x7FFFFFFFL);
    }

    public static int getOffset(
        final long addr
    ) {
        return (int)addr;
    }

    public static int getSegment(
        final long addr
    ) {
        return (int)(addr >> 32);
    }

    public static long addOffset(
        final long address,
        final int offset
    ) {
        return ((long)getSegment(address) << 32) | ((getOffset(address) + offset) & 0x7FFFFFFFL);
    }

    public static long setOffset(
        final long address,
        final int offset
    ) {
        return ((long)getSegment(address) << 32) | (offset & 0x7FFFFFFFL);
    }

    public static long setSegment(
        final long address,
        final int segment
    ) {
        return (((long)(segment & 0x7FFFFFFF)) << 32) | (getOffset(address) & 0xFFFFFFFFL);
    }

    public static String toString(
        final long address
    ) {
        return String.format("0%o:%012o", getSegment(address), getOffset(address));
    }
}
