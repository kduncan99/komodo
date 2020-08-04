/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.ConfigDataBank;
import com.kadware.komodo.hardwarelib.exceptions.StorageLockException;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Test;

public class Test_ConfigDataBank extends ConfigDataBank {

    static final long[] INITIAL_HEADER = {
        (0L << 18) | (1L),
        (((long) HEADER_SIZE) << 18) | DEFAULT_INITIAL_BANK_SIZE,
        HEADER_SIZE,                 //  mail slot table ref
        HEADER_SIZE,                 //  SP table ref
        HEADER_SIZE,                 //  IP table ref
        HEADER_SIZE,                 //  IOP table ref
        HEADER_SIZE,                 //  MSP table ref
        HEADER_SIZE,                 //  CM table ref
        HEADER_SIZE,                 //  dev table ref
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
    };

    @Before
    public void before() {
        clear();
    }

    @Test
    public void test_empty() {
        assertArrayEquals(INITIAL_HEADER, Arrays.copyOf(_arraySlice._array, INITIAL_HEADER.length));
    }

    @Test
    public void test_clear() {
        for (int wx = 0; wx < HEADER_SIZE; ++wx) {
            _arraySlice.set(wx, 0_755_666_557_343L);
        }
        clear();
        assertArrayEquals(INITIAL_HEADER, Arrays.copyOf(_arraySlice._array, INITIAL_HEADER.length));
    }

    @Test
    public void test_lock_and_clear(
    ) throws Exception {
        testSetLock();
        assertEquals(01, Word36.getS1(_arraySlice.get(0)));
        testSetUnlock();
        assertEquals(0, Word36.getS1(_arraySlice.get(0)));
    }

    @Test
    public void test_lock_fail(
    ) throws Exception {
        testSetLock();
        assertThrows(StorageLockException.class, this::testSetLock);
        testSetUnlock();
    }

    @Test
    public void test_expandArray() {
        int originalSize = getArraySize();
        int originalUsed = getArrayUsed();
        expandArray(-220);
        assertEquals(originalSize, getArraySize());
        assertEquals(originalUsed, getArrayUsed());

        expandArray(0);
        assertEquals(originalSize, getArraySize());
        assertEquals(originalUsed, getArrayUsed());

        expandArray(1);
        assertEquals(originalSize + 1024, getArraySize());
        assertEquals(originalSize + 1024, _arraySlice._array.length);
        assertEquals(originalUsed, getArrayUsed());
    }

    @Test
    public void test_expandTable() {
        int additionalEntries = 5;
        int additionalSize = additionalEntries * MAIL_SLOT_ENTRY_SIZE;
        int originalArraySize = getArraySize();
        int originalArrayUsed = getArrayUsed();
        int originalMailSlotOffset = getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET);
        int originalDeviceOffset = getTableOffset(DEVICE_TABLE_REFERENCE_OFFSET);
        int originalDeviceCount = getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET);

        expandTable(MAIL_SLOT_TABLE_REFERENCE_OFFSET, additionalEntries);
        assertEquals(originalArraySize, getArraySize());
        assertEquals(originalArrayUsed + additionalSize, getArrayUsed());
        assertEquals(originalMailSlotOffset, getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
        assertEquals(5, getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET));

        assertEquals(originalDeviceOffset + additionalSize, getTableOffset(DEVICE_TABLE_REFERENCE_OFFSET));
        assertEquals(originalDeviceCount, getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET));
    }

    //TODO expand some middle table forcing array expansion

    @Test
    public void test_populate(
    ) throws Exception {
//        InventoryManager im = InventoryManager.getInstance();
//        im.clearConfiguration();
//
//        im.createSystemProcessor("SP0", 0, 0, null);
//        im.createMainStorageProcessor("MSP0", MainStorageProcessor.MIN_FIXED_SIZE);
//        im.createMainStorageProcessor("MSP1", MainStorageProcessor.MIN_FIXED_SIZE);
//        im.createMainStorageProcessor("MSP2", MainStorageProcessor.MIN_FIXED_SIZE);
//        im.createInstructionProcessor("IP0");
//        im.createInstructionProcessor("IP1");
//        im.createInputOutputProcessor("IOP0");
//
//        ConfigDataBank cdb = new ConfigDataBank();
//        cdb.populate();
//
//        long mailSlots = InventoryManager.MAX_IOPS * InventoryManager.MAX_IPS;
//        InventoryManager.Counters counters = im.getCounters();
//        int mailSlotTableOffset = ConfigDataBank.HEADER_SIZE;
//
//        assertEquals(mailSlots, getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
//        assertEquals(mailSlotTableOffset, getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
//
//        long[] expectedArray = {
//            ConfigDataBank.DEFAULT_INITIAL_BANK_SIZE,   //  CDB size
//            ConfigDataBank.HEADER_SIZE,                 //  CDB usage
//            (mailSlots << 30) | ConfigDataBank.HEADER_SIZE,                 //  mail slot table ref
//            ConfigDataBank.HEADER_SIZE,                 //  SP table ref
//            ConfigDataBank.HEADER_SIZE,                 //  IP table ref
//            ConfigDataBank.HEADER_SIZE,                 //  IOP table ref
//            ConfigDataBank.HEADER_SIZE,                 //  MSP table ref
//            ConfigDataBank.HEADER_SIZE,                 //  CM table ref
//            ConfigDataBank.HEADER_SIZE,                 //  dev table ref
//        };
    }
}
