/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.mfd.FileAllocation;
import com.bearsnake.komodo.kexec.mfd.FileAllocationSet;
import com.bearsnake.komodo.kexec.mfd.LogicalTrackExtent;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestFileAllocationSet {

    private static final MFDRelativeAddress DAD_ITEM_0_ADDR = new MFDRelativeAddress(01001_0010_33);
    private static final MFDRelativeAddress MAIN_ITEM_0_ADDR = new MFDRelativeAddress(01001_0010_35);

    @Test
    public void testCtor() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);
        assertEquals(DAD_ITEM_0_ADDR, fas.getDadItem0Address());
        assertEquals(MAIN_ITEM_0_ADDR, fas.getMainItem0Address());
    }

    @Test
    public void testMergeIntoEmpty() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 10);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var allocs = fas.getFileAllocations();
        assertEquals(1, allocs.size());
        var entry = allocs.getFirst();
        assertEquals(region, entry.getFileRegion());
        assertEquals(hwTid, entry.getHardwareTrackId());
    }

    @Test
    public void testMergeAheadNonContiguousFileAddress() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 10);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(10, 2);
        var newHWTid = new HardwareTrackId(02, 990);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(2, allocs.size());

        var chk0 = allocs.get(0);
        assertEquals(newRegion, chk0.getFileRegion());
        assertEquals(newHWTid, chk0.getHardwareTrackId());

        var chk1 = allocs.get(1);
        assertEquals(region, chk1.getFileRegion());
        assertEquals(hwTid, chk1.getHardwareTrackId());
    }

    @Test
    public void testMergeAheadContiguous() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 10);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(10, 10);
        var newHWTid = new HardwareTrackId(02, 990);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(1, allocs.size());

        var chk0 = allocs.get(0);
        var chkRegion = new LogicalTrackExtent(10, 20);
        var chkHWTid = new HardwareTrackId(02, 990);
        assertEquals(chkRegion, chk0.getFileRegion());
        assertEquals(chkHWTid, chk0.getHardwareTrackId());
    }

    @Test
    public void testMergeBehindNonContiguousFileAddress() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 5);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(30, 2);
        var newHWTid = new HardwareTrackId(02, 1010);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(2, allocs.size());

        var chk0 = allocs.get(0);
        assertEquals(region, chk0.getFileRegion());
        assertEquals(hwTid, chk0.getHardwareTrackId());

        var chk1 = allocs.get(1);
        assertEquals(newRegion, chk1.getFileRegion());
        assertEquals(newHWTid, chk1.getHardwareTrackId());
    }

    @Test
    public void testMergeBehindNonContiguousDifferentLDAT() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 10);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(30, 2);
        var newHWTid = new HardwareTrackId(03, 1010);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(2, allocs.size());

        var chk0 = allocs.get(0);
        assertEquals(region, chk0.getFileRegion());
        assertEquals(hwTid, chk0.getHardwareTrackId());

        var chk1 = allocs.get(1);
        assertEquals(newRegion, chk1.getFileRegion());
        assertEquals(newHWTid, chk1.getHardwareTrackId());
    }

    @Test
    public void testMergeBehindNonContiguousPhysical() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 10);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(30, 2);
        var newHWTid = new HardwareTrackId(02, 1021);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(2, allocs.size());

        var chk0 = allocs.get(0);
        assertEquals(region, chk0.getFileRegion());
        assertEquals(hwTid, chk0.getHardwareTrackId());

        var chk1 = allocs.get(1);
        assertEquals(newRegion, chk1.getFileRegion());
        assertEquals(newHWTid, chk1.getHardwareTrackId());
    }

    @Test
    public void testMergeBehindContiguous() {
        var fas = new FileAllocationSet(DAD_ITEM_0_ADDR, MAIN_ITEM_0_ADDR);

        var region = new LogicalTrackExtent(20, 5);
        var hwTid = new HardwareTrackId(02, 1000);
        var fa = new FileAllocation(region, hwTid);
        fas.mergeIntoFileAllocationSet(fa);

        var newRegion = new LogicalTrackExtent(25, 2);
        var newHWTid = new HardwareTrackId(02, 1005);
        var newFA = new FileAllocation(newRegion, newHWTid);
        fas.mergeIntoFileAllocationSet(newFA);

        var allocs = fas.getFileAllocations();
        assertEquals(1, allocs.size());

        var chk0 = allocs.get(0);
        var chkRegion = new LogicalTrackExtent(20, 7);
        var chkHWTid = new HardwareTrackId(02, 1000);
        assertEquals(chkRegion, chk0.getFileRegion());
        assertEquals(chkHWTid, chk0.getHardwareTrackId());
    }
}
