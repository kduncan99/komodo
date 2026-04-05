/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAbsoluteAddress {

    @Test
    public void testConstructorAndGetters() {
        AbsoluteAddress aa = new AbsoluteAddress( 2, 3);
        assertEquals(2, aa.getSegment());
        assertEquals(3, aa.getOffset());
    }

    @Test
    public void testSetters() {
        AbsoluteAddress aa = new AbsoluteAddress(0, 0);
        aa.setSegment(10).setOffset(15);
        assertEquals(10, aa.getSegment());
        assertEquals(15, aa.getOffset());
    }

    @Test
    public void testLongArrayConstructor() {
        long[] data = new long[2];
        data[0] = 0x12345678L; // segment
        data[1] = 0x87654321L; // upiIndex | offset
        AbsoluteAddress aa = new AbsoluteAddress(data, 0);
        assertEquals(0x12345678, aa.getSegment());
        assertEquals(0x07654321, aa.getOffset());
    }

    @Test
    public void testAddOffset() {
        AbsoluteAddress aa = new AbsoluteAddress(100, 500);
        AbsoluteAddress aa2 = aa.addOffset(1000);
        assertEquals(100, aa2.getSegment());
        assertEquals(1500, aa2.getOffset());
    }

    @Test
    public void testEqualsAndHashCode() {
        AbsoluteAddress aa1 = new AbsoluteAddress(2, 3);
        AbsoluteAddress aa2 = new AbsoluteAddress(2, 3);
        AbsoluteAddress aa3 = new AbsoluteAddress(5, 6);

        assertEquals(aa1, aa2);
        assertNotEquals(aa1, aa3);
        assertEquals(aa1.hashCode(), aa2.hashCode());
    }

    @Test
    public void testToString() {
        AbsoluteAddress aa = new AbsoluteAddress(02, 01234567);
        // Format: "0%o:%012o"
        String expected = "02:000001234567";
        assertEquals(expected, aa.toString());
    }
}
