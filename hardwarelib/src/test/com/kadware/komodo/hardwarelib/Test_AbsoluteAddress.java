/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AbsoluteAddress;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests AbsoluteAddress class - not much to minalib here
 */
public class Test_AbsoluteAddress {

    @Test
    public void testEquals_false1(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)2, 0, 01000);
        assertNotEquals(a1, a2);
    }

    @Test
    public void testEquals_false2(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)1, 0, 02000);
        assertNotEquals(a1, a2);
    }

    @Test
    public void testEquals_false3(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)1, 010, 02000);
        assertNotEquals(a1, a2);
    }

    @Test
    public void testEquals_true(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)1, 0, 01000);
        assertEquals(a1, a2);
    }

    @Test
    public void testCollection_false(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)2, 0, 01000);
        Set<AbsoluteAddress> set = new HashSet<>();
        set.add(a1);
        assertFalse(set.contains(a2));
    }

    @Test
    public void testCollection_true(
    ) {
        AbsoluteAddress a1 = new AbsoluteAddress((short)1, 0, 01000);
        AbsoluteAddress a2 = new AbsoluteAddress((short)1, 0, 01000);
        Set<AbsoluteAddress> set = new HashSet<>();
        set.add(a1);
        assertTrue(set.contains(a2));
    }
}
