/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAbsoluteAddress {
    @Test
    public void testEncodeToLong() {
        assertEquals(0x0000000100000002L, AbsoluteAddress.encodeToLong(1, 2));
        assertEquals(0x7FFFFFFF00000000L, AbsoluteAddress.encodeToLong(0x7FFFFFFF, 0));
        assertEquals(0x000000007FFFFFFFL, AbsoluteAddress.encodeToLong(0, 0x7FFFFFFF));
        assertEquals(0x7FFFFFFF7FFFFFFFL, AbsoluteAddress.encodeToLong(0x7FFFFFFF, 0x7FFFFFFF));
        // Test masking
        assertEquals(0x7FFFFFFF7FFFFFFFL, AbsoluteAddress.encodeToLong(0xFFFFFFFF, 0xFFFFFFFF));
    }

    @Test
    public void testEncodeToStorage() {
        long[] storage = new long[4];
        AbsoluteAddress.encodeToStorage(0x12345678, 0x77665544, storage, 1);
        assertEquals(0x12345678L, storage[1]);
        assertEquals(0x77665544L, storage[2]);
        assertEquals(0L, storage[0]);
        assertEquals(0L, storage[3]);

        // Test masking
        AbsoluteAddress.encodeToStorage(0xFFFFFFFF, 0xFFFFFFFF, storage, 0);
        assertEquals(0x7FFFFFFFL, storage[0]);
        assertEquals(0x7FFFFFFFL, storage[1]);
    }

    @Test
    public void testExtractFromStorage() {
        long[] storage = {0, 0x11223344L, 0x55667788L, 0};
        assertEquals(0x11223344, AbsoluteAddress.extractSegmentFromStorage(storage, 1));
        assertEquals(0x55667788, AbsoluteAddress.extractOffsetFromStorage(storage, 1));

        long[] storageMask = {0xFFFFFFFFL, 0xFFFFFFFFL};
        assertEquals(0x7FFFFFFF, AbsoluteAddress.extractSegmentFromStorage(storageMask, 0));
        assertEquals(0x7FFFFFFF, AbsoluteAddress.extractOffsetFromStorage(storageMask, 0));
    }

    @Test
    public void testExtractFromLong() {
        long addr = 0x1234567800000001L;
        assertEquals(0x12345678, AbsoluteAddress.extractSegmentFromLong(addr));
        assertEquals(0x00000001, AbsoluteAddress.extractOffsetFromLong(addr));

        long addr2 = 0x7FFFFFFF7FFFFFFFL;
        assertEquals(0x7FFFFFFF, AbsoluteAddress.extractSegmentFromLong(addr2));
        assertEquals(0x7FFFFFFF, AbsoluteAddress.extractOffsetFromLong(addr2));
    }

    @Test
    public void testAddOffsetToLong() {
        long addr = AbsoluteAddress.encodeToLong(10, 100);
        long result = AbsoluteAddress.addOffsetToLong(addr, 50);
        assertEquals(10, AbsoluteAddress.extractSegmentFromLong(result));
        assertEquals(150, AbsoluteAddress.extractOffsetFromLong(result));

        long result2 = AbsoluteAddress.addOffsetToLong(addr, -50);
        assertEquals(10, AbsoluteAddress.extractSegmentFromLong(result2));
        assertEquals(50, AbsoluteAddress.extractOffsetFromLong(result2));

        // Test wrap around behavior of the offset field (31 bits)
        long addrMax = AbsoluteAddress.encodeToLong(1, 0x7FFFFFFF);
        long result3 = AbsoluteAddress.addOffsetToLong(addrMax, 1);
        assertEquals(1, AbsoluteAddress.extractSegmentFromLong(result3));
        assertEquals(0, AbsoluteAddress.extractOffsetFromLong(result3)); // 0x80000000 masked to 31 bits is 0
    }

    @Test
    public void testToString() {
        long addr = AbsoluteAddress.encodeToLong(012, 0123456);
        // Expecting octal format "0<segment>:<offset>"
        // segment 012 -> 12 octal, offset 0123456 -> 123456 octal
        // format is "0%o:%012o"
        String expected = String.format("0%o:%012o", 012, 0123456);
        assertEquals(expected, AbsoluteAddress.toString(addr));
    }
}
