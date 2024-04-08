/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestHardwareTrackId {

    @Test
    public void testBasic() {
        var tid = new HardwareTrackId(05, 1000);
        assertEquals(5, tid.getLDATIndex());
        assertEquals(1000, tid.getTrackId());
    }

    @Test
    public void testEquals() {
        var tid1 = new HardwareTrackId(3, 500);
        var tid2 = new HardwareTrackId(3, 500);
        assertEquals(tid1, tid2);
        assertEquals(tid2, tid1);
    }

    @Test
    public void testEqualsDifferentLDAT() {
        var tid1 = new HardwareTrackId(3, 500);
        var tid2 = new HardwareTrackId(4, 500);
        assertNotEquals(tid1, tid2);
        assertNotEquals(tid2, tid1);
    }

    @Test
    public void testEqualsDifferentTrackId() {
        var tid1 = new HardwareTrackId(4, 500);
        var tid2 = new HardwareTrackId(4, 501);
        assertNotEquals(tid1, tid2);
        assertNotEquals(tid2, tid1);
    }

    @Test
    public void testIsContiguousTo() {
        var tid1 = new HardwareTrackId(4, 500);
        var tid2 = new HardwareTrackId(4, 505);
        assertTrue(tid2.isContiguousTo(tid1, 5));
        assertFalse(tid1.isContiguousTo(tid2, 5));
        assertFalse(tid2.isContiguousTo(tid1, 4));
        assertFalse(tid2.isContiguousTo(tid1, 6));
    }

    @Test
    public void testIsContiguousDifferentLDAT() {
        var tid1 = new HardwareTrackId(4, 500);
        var tid2 = new HardwareTrackId(3, 505);
        assertFalse(tid2.isContiguousTo(tid1, 5));
    }

    @Test
    public void testIsContiguousGap() {
        var tid1 = new HardwareTrackId(4, 500);
        var tid2 = new HardwareTrackId(4, 510);
        assertFalse(tid2.isContiguousTo(tid1, 5));
    }

    @Test
    public void testIsContiguousOverlap() {
        var tid1 = new HardwareTrackId(4, 500);
        var tid2 = new HardwareTrackId(4, 510);
        assertFalse(tid2.isContiguousTo(tid1, 11));
    }
}
