/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.mfd.FileAllocation;
import com.bearsnake.komodo.kexec.mfd.LogicalTrackExtent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestFileAllocation {

    @Test
    public void testBasic() {
        var region = new LogicalTrackExtent(500, 10);
        var hwTid = new HardwareTrackId(01001, 1000);
        var fa = new FileAllocation(region, hwTid);
        assertEquals(region, fa.getFileRegion());
        assertEquals(hwTid, fa.getHardwareTrackId());
    }

    @Test
    public void testContiguous() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(510, 10);
        var hwTid2 = new HardwareTrackId(01001, 1010);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertTrue(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }

    @Test
    public void testContiguousDifferentLDATs() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(510, 10);
        var hwTid2 = new HardwareTrackId(01002, 1010);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertFalse(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }

    @Test
    public void testContiguousLogicalGap() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(511, 10);
        var hwTid2 = new HardwareTrackId(01001, 1010);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertFalse(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }

    @Test
    public void testContiguousPhysicalGap() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(510, 10);
        var hwTid2 = new HardwareTrackId(01001, 1011);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertFalse(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }

    @Test
    public void testContiguousLogicalOverlap() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(509, 10);
        var hwTid2 = new HardwareTrackId(01001, 1010);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertFalse(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }

    @Test
    public void testContiguousPhysicalOverlap() {
        var region1 = new LogicalTrackExtent(500, 10);
        var hwTid1 = new HardwareTrackId(01001, 1000);
        var fa1 = new FileAllocation(region1, hwTid1);

        var region2 = new LogicalTrackExtent(510, 10);
        var hwTid2 = new HardwareTrackId(01001, 1009);
        var fa2 = new FileAllocation(region2, hwTid2);

        assertFalse(fa2.isContiguousTo(fa1));
        assertFalse(fa1.isContiguousTo(fa2));
    }
}
