/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.ConfigDataBank;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.MainStorageProcessor;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Test_ConfigDataBank extends ConfigDataBank {

    static final long[] _defaultHeader = {
        ConfigDataBank.DEFAULT_INITIAL_BANK_SIZE,   //  CDB size
        ConfigDataBank.HEADER_SIZE,                 //  CDB usage
        ConfigDataBank.HEADER_SIZE,                 //  mail slot table ref
        ConfigDataBank.HEADER_SIZE,                 //  SP table ref
        ConfigDataBank.HEADER_SIZE,                 //  IP table ref
        ConfigDataBank.HEADER_SIZE,                 //  IOP table ref
        ConfigDataBank.HEADER_SIZE,                 //  MSP table ref
        ConfigDataBank.HEADER_SIZE,                 //  CM table ref
        ConfigDataBank.HEADER_SIZE,                 //  dev table ref
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

    @Test
    public void test_empty() {
        assertArrayEquals(_defaultHeader, Arrays.copyOf(getStorage()._array, _defaultHeader.length));
    }

    //TODO need unit tests of many of the helper functions - something is wrong there (at least one thing, anyway)

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
//    }
}
