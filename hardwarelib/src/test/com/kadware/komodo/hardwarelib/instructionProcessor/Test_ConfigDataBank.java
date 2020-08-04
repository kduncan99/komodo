/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.ConfigDataBank;
import com.kadware.komodo.hardwarelib.InputOutputProcessor;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.MainStorageProcessor;
import com.kadware.komodo.hardwarelib.Processor;
import com.kadware.komodo.hardwarelib.SystemProcessor;
import com.kadware.komodo.hardwarelib.exceptions.StorageLockException;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
    public void test_nameWords() {
        String name1 = "A";
        long[] nameWords1 = { 0_101040040040L, 0_040040040040L };
        String name2 = "abc123";
        long[] nameWords2 = { 0_101102103061L, 0_062063040040L };
        String name3 = "oA0123456789";
        long[] nameWords3 = { 0_117101060061L, 0_062063064065L };

        assertArrayEquals(nameWords1, getNameWords(name1));
        assertArrayEquals(nameWords2, getNameWords(name2));
        assertArrayEquals(nameWords3, getNameWords(name3));
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
        assertEquals(additionalEntries, getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET));

        assertEquals(originalDeviceOffset + additionalSize, getTableOffset(DEVICE_TABLE_REFERENCE_OFFSET));
        assertEquals(originalDeviceCount, getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET));
    }

    @Test
    public void test_expandTableTwice() {
        int additionalEntries = 5;
        int additionalSize = additionalEntries * MAIL_SLOT_ENTRY_SIZE;
        int originalArraySize = getArraySize();
        int originalArrayUsed = getArrayUsed();
        int originalMailSlotOffset = getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET);
        int originalDeviceOffset = getTableOffset(DEVICE_TABLE_REFERENCE_OFFSET);
        int originalDeviceCount = getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET);

        expandTable(MAIL_SLOT_TABLE_REFERENCE_OFFSET, additionalEntries);
        expandTable(MAIL_SLOT_TABLE_REFERENCE_OFFSET, additionalEntries);
        assertEquals(originalArraySize, getArraySize());
        assertEquals(originalArrayUsed + 2 * additionalSize, getArrayUsed());
        assertEquals(originalMailSlotOffset, getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
        assertEquals(2 * additionalEntries, getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET));

        assertEquals(originalDeviceOffset + 2 * additionalSize, getTableOffset(DEVICE_TABLE_REFERENCE_OFFSET));
        assertEquals(originalDeviceCount, getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET));
    }

    //TODO expand table ahead of some other non-empty table

    //TODO expand some middle table forcing array expansion, ahead of some other non-empty table

    //TODO populate various entry types

    //TODO establish some mail slots

    //TODO add various entry types

    @Test
    public void test_populate(
    ) throws Exception {
        InventoryManager im = InventoryManager.getInstance();
        im.clearConfiguration();

        String sp0Name = "SP0";
        String msp0Name = "MSP0";
        String msp1Name = "MSP1";
        String msp2Name = "MSP2";
        String ip0Name = "IP0";
        String ip1Name = "IP1";
        String iop0Name = "IOP0";

        SystemProcessor sp0 = im.createSystemProcessor(sp0Name, 0, 0, null);
        MainStorageProcessor msp0 = im.createMainStorageProcessor(msp0Name, MainStorageProcessor.MIN_FIXED_SIZE);
        MainStorageProcessor msp1 = im.createMainStorageProcessor(msp1Name, MainStorageProcessor.MIN_FIXED_SIZE);
        MainStorageProcessor msp2 = im.createMainStorageProcessor(msp2Name, MainStorageProcessor.MIN_FIXED_SIZE);
        InstructionProcessor ip0 = im.createInstructionProcessor(ip0Name);
        InstructionProcessor ip1 = im.createInstructionProcessor(ip1Name);
        InputOutputProcessor iop0 = im.createInputOutputProcessor(iop0Name);
        //TODO chmods and devices

        populate(im);
        dump(System.out);

        InventoryManager.Counters counters = im.getCounters();

        assertEquals(0, getTestSetCell());
        assertEquals(010, getConfigurationNumber());
        assertEquals(DEFAULT_INITIAL_BANK_SIZE, getArraySize());

        int expectedMailSlotTableOffset = HEADER_SIZE;
        int expectedMailSlotEntries = counters._inputOutputProcessors * counters._instructionProcessors;
        int mailSlotTableSize = expectedMailSlotEntries * MAIL_SLOT_ENTRY_SIZE;
        assertEquals(expectedMailSlotTableOffset, getTableOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
        assertEquals(expectedMailSlotEntries, getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET));
        assertNotEquals(0, findMailSlotEntry(ip0._upiIndex, iop0._upiIndex));
        assertNotEquals(0, findMailSlotEntry(ip1._upiIndex, iop0._upiIndex));

        int expectedSystemProcessorTableOffset = expectedMailSlotTableOffset + mailSlotTableSize;
        int expectedSystemProcessorEntries = counters._systemProcessors;
        int systemProcessorTableSize = expectedSystemProcessorEntries * SYSTEM_PROCESSOR_ENTRY_SIZE;
        assertEquals(expectedSystemProcessorTableOffset, getTableOffset(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET));
        assertEquals(expectedSystemProcessorEntries, getTableEntryCount(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET));
        int sp0EntryOffset = findSystemProcessorEntry(sp0);
        assertEquals(expectedSystemProcessorTableOffset, sp0EntryOffset);
        assertEquals(NODE_STATE_ACTIVE, getNodeEntryState(sp0EntryOffset));
        assertEquals(Processor.ProcessorType.SystemProcessor.getCode(), getNodeEntryType(sp0EntryOffset));
        assertEquals(0, getNodeEntryModel(sp0EntryOffset));
        assertArrayEquals(getNameWords(sp0Name), getNodeEntryName(sp0EntryOffset));
        assertEquals(sp0._upiIndex, getProcessorEntryUPIIndex(sp0EntryOffset));

        int expectedInstructionProcessorTableOffset = expectedSystemProcessorTableOffset + systemProcessorTableSize;
        int expectedInstructionProcessorEntries = counters._instructionProcessors;
        int instructionProcessorTableSize = expectedInstructionProcessorEntries * INSTRUCTION_PROCESSOR_ENTRY_SIZE;
        assertEquals(expectedInstructionProcessorTableOffset, getTableOffset(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET));
        assertEquals(expectedInstructionProcessorEntries, getTableEntryCount(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET));
        //TODO ips specific info

        int expectedInputOutputProcessorTableOffset = expectedInstructionProcessorTableOffset + instructionProcessorTableSize;
        int expectedInputOutputProcessorEntries = counters._inputOutputProcessors;
        int inputOutputProcessorTableSize = expectedInputOutputProcessorEntries * INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE;
        assertEquals(expectedInputOutputProcessorTableOffset, getTableOffset(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET));
        assertEquals(expectedInputOutputProcessorEntries, getTableEntryCount(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET));
        //TODO iops specific info

        int expectedMainStorageProcessorTableOffset = expectedInputOutputProcessorTableOffset + inputOutputProcessorTableSize;
        int expectedMainStorageProcessorEntries = counters._mainStorageProcessors;
        int mainStorageProcessorTableSize = expectedMainStorageProcessorEntries * MAIN_STORAGE_PROCESSOR_ENTRY_SIZE;
        assertEquals(expectedMainStorageProcessorTableOffset, getTableOffset(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET));
        assertEquals(expectedMainStorageProcessorEntries, getTableEntryCount(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET));
        //TODO msps specific info

        //TODO chmdos

        //TODO devices
    }
}
