/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAbsoluteAddress {

    @Test
    public void testConstructorAndGetters() {
        var aa = AbsoluteAddress.construct(2, 3);
        assertEquals(2, AbsoluteAddress.getSegment(aa));
        assertEquals(3, AbsoluteAddress.getOffset(aa));
    }

    @Test
    public void testSetters() {
        long aa = AbsoluteAddress.construct(0, 0);
        aa = AbsoluteAddress.setSegment(aa, 10);
        aa = AbsoluteAddress.setOffset(aa, 15);
        assertEquals(10, AbsoluteAddress.getSegment(aa));
        assertEquals(15, AbsoluteAddress.getOffset(aa));
    }

    @Test
    public void testAddOffset() {
        var aa = AbsoluteAddress.construct(100, 500);
        long aa2 = AbsoluteAddress.addOffset(aa, 1000);
        assertEquals(100, AbsoluteAddress.getSegment(aa2));
        assertEquals(1500, AbsoluteAddress.getOffset(aa2));
    }

    @Test
    public void testToString() {
        var aa = AbsoluteAddress.construct(02, 01234567);
        // Format: "0%o:%012o"
        String expected = "02:000001234567";
        assertEquals(expected, AbsoluteAddress.toString(aa));
    }
}
