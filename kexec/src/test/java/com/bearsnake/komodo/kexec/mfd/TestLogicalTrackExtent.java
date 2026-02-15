/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLogicalTrackExtent {

    @Test
    public void testBasic() {
        var region = new LogicalTrackExtent(500, 10);
        assertEquals(500, region.getTrackId());
        assertEquals(10, region.getTrackCount());
    }

    @Test
    public void testEquals() {
        var region1 = new LogicalTrackExtent(250, 25);
        var region2 = new LogicalTrackExtent(250, 25);
        assertEquals(region1, region2);
        assertEquals(region2, region1);
    }

    @Test
    public void testEqualsDifferingTrackId() {
        var region1 = new LogicalTrackExtent(250, 25);
        var region2 = new LogicalTrackExtent(251, 25);
        assertNotEquals(region1, region2);
        assertNotEquals(region2, region1);
    }

    @Test
    public void testEqualsDifferingTrackCount() {
        var region1 = new LogicalTrackExtent(250, 2);
        var region2 = new LogicalTrackExtent(250, 5);
        assertNotEquals(region1, region2);
        assertNotEquals(region2, region1);
    }

    @Test
    public void testContiguous() {
        var region1 = new LogicalTrackExtent(53, 12);
        var region2 = new LogicalTrackExtent(65, 5);
        assertTrue(region2.isContiguousTo(region1));
        assertFalse(region1.isContiguousTo(region2));
    }

    @Test
    public void testContiguousGap() {
        var region1 = new LogicalTrackExtent(53, 12);
        var region2 = new LogicalTrackExtent(75, 5);
        assertFalse(region2.isContiguousTo(region1));
        assertFalse(region1.isContiguousTo(region2));
    }

    @Test
    public void testContiguousOverlap() {
        var region1 = new LogicalTrackExtent(53, 12);
        var region2 = new LogicalTrackExtent(64, 5);
        assertFalse(region2.isContiguousTo(region1));
        assertFalse(region1.isContiguousTo(region2));
    }
}
