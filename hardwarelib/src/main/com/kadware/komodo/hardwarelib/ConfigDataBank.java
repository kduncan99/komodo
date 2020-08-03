/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.hardwarelib.exceptions.StorageLockException;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the configuration data bank.
 *
 * The content consists of a number of 36-bit words wrapped in longs.
 * The format is as follows:
 *
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      | TS Cell  |                     |      Configuration Number      |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |   current CDB usage in words   |       CDB size in words        |
 *              +----------+----------+----------+----------+----------+----------+
 *              |   # of   |                     |        offset of first         |
 *     + 2      |mail slot |                     |        mail slot entry         |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      | # of SP  |                     |    offset of first SP entry    |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 4      | # of IP  |                     |    offset of first IP entry    |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 5      | # of IOP |                     |    offset of first IOP entry   |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 6      | # of MSP |                     |    offset of first MSP entry   |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 7      |   # of   |                     |        offset of first         |
 *              |  chmod   |                     |      channel module entry      |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 8      | # of dev |                     |        offset of first         |
 *              | entries  |                     |         device entry           |
 *              +----------+----------+----------+----------+----------+----------+
 *                                             . . .
 *              +----------+----------+----------+----------+----------+----------+
 *     + 31     |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *
 *  Format of a mail slot entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |  state   |          |  source UPI index   |   dest UPI index    |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                              mail                               |
 *     + 2      |                              slot                               |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      It is likely that we only need IPs for sources and IOPs for destinations
 *
 *
 *  Format of an SP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |          |     UPI index       |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                            SP name                              |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   0 = emulated SP
 *
 *      model:  Always 0 for now
 *
 *
 *  Format of an IP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |          |     UPI index       |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                            IP name                              |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active (operational, but not executing instructions - halted)
 *              03 = running (i.e., executing instructions)
 *
 *      type:   01 = emulated IP
 *
 *      model:  Always 0 for now
 *
 *
 *  Format of an IOP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |          |     UPI index       |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                            IOP name                             |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating System state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *     +5       |  inuse   |                     |  CDB offset of channel module  |
 *              +----------+----------+----------+----------+----------+----------+
 *              +----------+----------+----------+----------+----------+----------+
 *     +5+n     |  inuse   |                     |  CDB offset of channel module  |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   02 = emulated IOP
 *
 *      model:  Always 0 for now
 *
 *      Words 5 through 5+n reflect connected channel modules at slots 0 through n,
 *      where n is limited by the max number of channel modules per IOP.
 *          inuse:  00 if no channel module is connected, H2 should be ignored
 *                  01 if a channel module is connected and H2 is relevant
 *
 *
 *  Format of an MSP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |          |     UPI index       |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                            MSP name                             |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 5      |                    size of MSP fixed storage                    |
 *              |                             in words                            |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 6      |                   number of dynamic segments                    |
 *              |                     which can be allocated                      |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (MSPs are never active)
 *
 *      type:   03 = emulated MSP
 *
 *      model:  Always 0 for now
 *
 *
 *  Format of a Channel Module entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                       Channel Module name                       |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *     +5       |  inuse   |                     |  CDB offset of device entry 0  |
 *              +----------+----------+----------+----------+----------+----------+
 *              +----------+----------+----------+----------+----------+----------+
 *     +5+n     |  inuse   |                     |  CDB offset of device entry n  |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   01 = byte channel module
 *              02 = word channel module
 *              (see ChannelModule.java for the corresponding enum)
 *
 *      model:  Always 0 for now
 *
 *      Words 5 through 5+n reflect connected devices at indices 0 througn n,
 *      where n is limited by the max number of devices per channel module.
 *          inuse:  00 if no channel module is connected, H2 should be ignored
 *                  01 if a channel module is connected and H2 is relevant
 *
 *
 *  Format of a Device entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |   state  |   type   |  model   |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                           Device name                           |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   (see DiskDevice.java for the corresponding enum)
 *              01 = disk
 *              02 = symbiont
 *              03 = tape
 *
 *      model:  (see DiskDevice.java for the corresponding enum)
 *
 *
 *  This gets populated by SP at IPL time, in one shot.
 *  It is encapsulated in the Processor base class for the convenience of the various processor objects.
 *  This bank is built by the SP just prior to ipl().
 *  It is always located at UPI:0 seg:0 offset:0, and may or may not be a fixed size.
 */
@SuppressWarnings("DuplicatedCode")
public class ConfigDataBank {

    public static final int DEFAULT_INITIAL_BANK_SIZE = 32 * 1024; //  32k words
    public static final int HEADER_SIZE = 32;

    protected static final int FIRST_TABLE_REFERENCE_OFFSET = 2;
    protected static final int MAIL_SLOT_TABLE_REFERENCE_OFFSET = 2;
    protected static final int MAIL_SLOT_ENTRY_SIZE = 3;
    protected static final int SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET = 3;
    protected static final int SYSTEM_PROCESSOR_ENTRY_SIZE = 5;
    protected static final int INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET = 4;
    protected static final int INSTRUCTION_PROCESSOR_ENTRY_SIZE = 5;
    protected static final int INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET = 5;
    protected static final int INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE = 5 + InventoryManager.MAX_CHANNEL_MODULES_PER_IOP;
    protected static final int MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET = 6;
    protected static final int MAIN_STORAGE_PROCESSOR_ENTRY_SIZE = 7;
    protected static final int CHANNEL_MODULE_TABLE_REFERENCE_OFFSET = 7;
    protected static final int CHANNEL_MODULE_ENTRY_SIZE = 5 + InventoryManager.MAX_DEVICES_PER_CHANNEL_MODULE;
    protected static final int DEVICE_TABLE_REFERENCE_OFFSET = 8;
    protected static final int DEVICE_ENTRY_SIZE = 5;
    protected static final int LAST_TABLE_REFERENCE_OFFSET = 8;

    protected static final int NODE_STATE_UNUSED = 0;
    protected static final int NODE_STATE_INACTIVE = 1;
    protected static final int NODE_STATE_ACTIVE = 2;
    protected static final int NODE_STATE_RUNNING = 3;

    protected static final int LOCK_TIMEOUT_MILLIS = 1000;

    private static final Map<Integer, Integer> _referenceOffsetToEntrySize = new HashMap<>();
    static {
        _referenceOffsetToEntrySize.put(MAIL_SLOT_TABLE_REFERENCE_OFFSET, MAIL_SLOT_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, SYSTEM_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, INSTRUCTION_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, MAIN_STORAGE_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, CHANNEL_MODULE_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(DEVICE_TABLE_REFERENCE_OFFSET, DEVICE_ENTRY_SIZE);
    }

    private ArraySlice _arraySlice;

    public ConfigDataBank() {
        _arraySlice = new ArraySlice(new long[DEFAULT_INITIAL_BANK_SIZE]);
        clear();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  very fundamental private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  The following each update some partial word of the array
    private void spliceH1(int offset, int value) { _arraySlice.set(offset, Word36.setH1(_arraySlice.get(offset), value)); }
    private void spliceH2(int offset, int value) { _arraySlice.set(offset, Word36.setH2(_arraySlice.get(offset), value)); }
    private void spliceS1(int offset, int value) { _arraySlice.set(offset, Word36.setS1(_arraySlice.get(offset), value)); }
    private void spliceS2(int offset, int value) { _arraySlice.set(offset, Word36.setS2(_arraySlice.get(offset), value)); }
    private void spliceS3(int offset, int value) { _arraySlice.set(offset, Word36.setS3(_arraySlice.get(offset), value)); }
    private void spliceT2(int offset, int value) { _arraySlice.set(offset, Word36.setT2(_arraySlice.get(offset), value)); }
    private void spliceT3(int offset, int value) { _arraySlice.set(offset, Word36.setT3(_arraySlice.get(offset), value)); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  fundamental private methods (protected for unit test access)
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  fundamental accessors of the header fields of the CDB
    //  *ALWAYS* use these, as we have not yet settled the format of the CDB, and any change thereto
    //  would be disruptive otherwise - this way we can make the accessor changes in one place and forget it.

    //  global stuff ---------------------------------------------------------------------------------------------------------------
    protected int getTestSetCell() { return (int) Word36.getS1(_arraySlice.get(0)); }
    protected void setTestSetCell(int value) { spliceS1(0 , value); }

    protected int getConfigurationNumber() { return (int) Word36.getH1(_arraySlice.get(0)); }
    protected void incrementConfigurationNumber() { setConfigurationNumber(getConfigurationNumber() + 1); }
    protected void setConfigurationNumber(int value) { spliceH2(0, value); }

    protected int getArrayUsed() { return (int) Word36.getH1(_arraySlice.get(1)); }
    protected void setArrayUsed(int value) { spliceH1(1, value); }

    protected int getArraySize() { return (int) Word36.getH2(_arraySlice.get(1)); }
    protected void setArraySize(int value) { spliceH2(1, value); }
    protected void updateArraySize() { setArraySize(_arraySlice._length); }

    //  table stuff ----------------------------------------------------------------------------------------------------------------
    protected int getTableSizeWords(
        final int tableReferenceOffset
    ) {
        return getTableEntryCount(tableReferenceOffset) * getTableEntrySizeWords(tableReferenceOffset);
    }

    protected int getTableEntryCount(int tableReferenceOffset) { return (int) Word36.getS1(_arraySlice.get(tableReferenceOffset)); }
    protected void setTableEntryCount(int tableReferenceOffset, int entryCount) { spliceS1(tableReferenceOffset, entryCount); }

    protected int getTableOffset(int tableReferenceOffset) { return (int) Word36.getH2(_arraySlice.get(tableReferenceOffset)); }
    protected void setTableOffset(int tableReferenceOffset, int tableOffset) { spliceH2(tableReferenceOffset, tableOffset); }

    //  table entry stuff ----------------------------------------------------------------------------------------------------------
    protected void clearTableEntry(
        final int tableReferenceOffset,
        final int entryIndex
    ) {
        int tableOffset = getTableOffset(tableReferenceOffset);
        int entryCount = getTableEntryCount(tableReferenceOffset);
        int entryOffset = tableOffset + entryIndex * _referenceOffsetToEntrySize.get(tableReferenceOffset);
        for (int ex = 0; ex < entryCount; ++ex, ++entryOffset) {
            _arraySlice.set(entryOffset, 0);
        }
    }

    protected int getTableEntryOffset(
        final int tableReferenceOffset,
        final int entryIndex
    ) {
        return getTableOffset(tableReferenceOffset) + entryIndex * getTableEntrySizeWords(tableReferenceOffset);
    }

    protected int getTableEntrySizeWords(int tableReferenceCount) {
        return _referenceOffsetToEntrySize.get(tableReferenceCount);
    }

    protected long[] getTableEntryWords(
        final int tableReferenceOffset,
        final int entryIndex,
        final int wordIndex,
        final int wordCount
    ) {
        int entryOffset = getTableEntryOffset(tableReferenceOffset, entryIndex);
        return Arrays.copyOfRange(_arraySlice._array, entryOffset + wordIndex, entryOffset + wordIndex + wordCount);
    }

    //  node entries common stuff --------------------------------------------------------------------------------------------------
    protected int getNodeEntryState(int entryOffset) { return (int) Word36.getS1(_arraySlice.get(entryOffset)); }
    protected void setNodeEntryState(int entryOffset, int value) { spliceS1(entryOffset, value); }
    protected int getNodeEntryType(int entryOffset) { return (int) Word36.getS2(_arraySlice.get(entryOffset)); }
    protected void setNodeEntryType(int entryOffset, int value) { spliceS2(entryOffset, value); }
    protected int getNodeEntryModel(int entryOffset) { return (int) Word36.getS3(_arraySlice.get(entryOffset)); }
    protected void setNodeEntryModel(int entryOffset, int value) { spliceS3(entryOffset, value); }

    protected long[] getNodeEntryName(
        final int entryOffset
    ) {
        return new long[]{
            _arraySlice.get(entryOffset + 1),
            _arraySlice.get(entryOffset + 2)
        };
    }

    protected void setNodeEntryName(
        final int entryOffset,
        final long[] words
    ) {
        _arraySlice.set(entryOffset + 1, words[0]);
        _arraySlice.set(entryOffset + 2, words[0]);
    }

    protected long[] getNodeEntryOperatingSystemState(
        final int entryOffset
    ) {
        return new long[]{
            _arraySlice.get(entryOffset + 3),
            _arraySlice.get(entryOffset + 4)
        };
    }

    protected void setNodeEntryOperatingSystemState(
        final int entryOffset,
        final long[] words
    ) {
        _arraySlice.set(entryOffset + 3, words[0]);
        _arraySlice.set(entryOffset + 4, words[0]);
    }

    //  processor common stuff -----------------------------------------------------------------------------------------------------
    protected int getProcessorEntryUPIIndex(int entryOffset) { return (int) Word36.getT3(_arraySlice.get(entryOffset)); }
    protected void setProcessorEntryUPIIndex(int entryOffset, int upiIndex) { spliceT3(entryOffset, upiIndex); }

    //  specials for IOP -----------------------------------------------------------------------------------------------------------
    protected boolean getInputOutputProcessorEntryChannelModuleConnected(
        int iopEntryOffset,
        int channelModuleIndex
    ) {
        return Word36.getS1(_arraySlice.get(iopEntryOffset + 5 + channelModuleIndex)) != 0;
    }

    protected void setInputOutputProcessorEntryChannelModuleConnected(
        int iopEntryOffset,
        int channelModuleIndex,
        boolean value
    ) {
        spliceS1(iopEntryOffset + 5 + channelModuleIndex, value ? 1 : 0);
    }

    protected int getInputOutputProcessorEntryChannelModuleReference(
        int iopEntryOffset,
        int channelModuleIndex
    ) {
        return (int) Word36.getH2(_arraySlice.get(iopEntryOffset + 5 + channelModuleIndex));
    }

    protected void setInputOutputProcessorEntryChannelModuleReference(
        int iopEntryOffset,
        int channelModuleIndex,
        int value
    ) {
        spliceH2(iopEntryOffset + 5 + channelModuleIndex, value);
    }

    //  specials for MSP -----------------------------------------------------------------------------------------------------------
    protected long getMainStorageProcessorEntryFixedSize(int entryOffset) { return _arraySlice.get(entryOffset + 5); }
    protected void setMainStorageProcessorEntryFixedSize(int entryOffset, long value) { _arraySlice.set(entryOffset, value); }
    protected long getMainStorageProcessorEntryTotalSegments(int entryOffset) { return _arraySlice.get(entryOffset + 6); }
    protected void setMainStorageProcessorEntryTotalSegments(int entryOffset, long value) { _arraySlice.set(entryOffset, value); }

    //  specials for channel module ------------------------------------------------------------------------------------------------
    protected boolean getChannelModuleEntryDeviceConnected(
        int chmodEntryOffset,
        int deviceIndex
    ) {
        return Word36.getS1(_arraySlice.get(chmodEntryOffset + 5 + deviceIndex)) != 0;
    }

    protected void setChannelModuleEntryDeviceConnected(
        int chmodEntryOffset,
        int deviceIndex,
        boolean value
    ) {
        spliceS1(chmodEntryOffset + 5 + deviceIndex, value ? 1 : 0);
    }

    protected int getChannelModuleEntryDeviceReference(
        int chmodEntryOffset,
        int deviceIndex
    ) {
        return (int) Word36.getH2(_arraySlice.get(chmodEntryOffset + 5 + deviceIndex));
    }

    protected void setChannelModuleEntryDeviceReference(
        int chmodEntryOffset,
        int deviceIndex,
        int value
    ) {
        spliceH2(chmodEntryOffset + 5 + deviceIndex, value);
    }

    //  mail slot entries ----------------------------------------------------------------------------------------------------------
    protected int getMailSlotEntrySourceUPIIndex(int entryOffset) { return (int) Word36.getT2(_arraySlice.get(entryOffset)); }
    protected void setMailSlotEntrySourceUPIIndex(int entryOffset, int upiIndex) { spliceT2(entryOffset, upiIndex); }

    protected int getMailSlotEntryDestinationUPIIndex(int entryOffset) { return (int) Word36.getT3(_arraySlice.get(entryOffset)); }
    protected void setMailSlotEntryDestinationUPIIndex(int entryOffset, int upiIndex) { spliceT3(entryOffset, upiIndex); }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods (protected for unit test access)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Clears all the configuration entries for the CDB
     */
    protected void clear(
    ) {
        setTestSetCell(0);
        setConfigurationNumber(1);
        updateArraySize();
        setArrayUsed(HEADER_SIZE);
        for (int refOffset = FIRST_TABLE_REFERENCE_OFFSET; refOffset <= LAST_TABLE_REFERENCE_OFFSET; ++refOffset) {
            setTableEntryCount(refOffset, 0);
            setTableOffset(refOffset, HEADER_SIZE);
        }
    }

    /**
     * Establishes a mail slot entry for the given source/destination combination.
     * If the slot already exists, no action is taken.
     * @param sourceUpiIndex UPI index of source processor
     * @param destinationUpiIndex UPI index of destination processor
     */
    protected void establishMailSlot(
        final int sourceUpiIndex,
        final int destinationUpiIndex
    ) {
        int entryOffset = findMailSlotEntry(sourceUpiIndex, destinationUpiIndex);
        if (entryOffset == 0) {
            //  one doesn't exist; create one.
            entryOffset = expandTable(MAIL_SLOT_TABLE_REFERENCE_OFFSET, 1);
            setMailSlotEntrySourceUPIIndex(entryOffset, sourceUpiIndex);
            setMailSlotEntryDestinationUPIIndex(entryOffset, destinationUpiIndex);
        }
    }

    /**
     * Expands the underlying array slice by at least the increment value, modulo 1k.
     */
    protected void expandArray(
        final int increment
    ) {
        int newSize = _arraySlice.getSize() + increment;
        newSize = (newSize & 0377) + 1;
        _arraySlice = _arraySlice.copyOf(_arraySlice.getSize() + newSize);
        setArraySize(newSize);
    }

    /**
     * Expands the table indicated by the reference offset by one or more entries
     * and clears the entry to all zeros including the state value (meaning unused)
     * @param tableReferenceOffset Offset from the start of the ConfigDataBank of the table reference
     *                       describing the table to be expanded.
     * @param additionalEntries number of new entries to be created
     * @return offset from the start of the Config Data Bank, of the first new entry
     */
    protected int expandTable(
        final int tableReferenceOffset,
        final int additionalEntries
    ) {
        if ((tableReferenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (tableReferenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        if (additionalEntries < 0) {
            throw new RuntimeException("Invalid additional entries value");
        }

        if (additionalEntries == 0) {
            return getTableOffset(tableReferenceOffset);
        }

        int entrySize = _referenceOffsetToEntrySize.get(tableReferenceOffset);
        int oldEntryCount = getTableEntryCount(tableReferenceOffset);
        int tableOffset = getTableOffset(tableReferenceOffset);

        int oldTableSizeWords = oldEntryCount * entrySize;
        int newEntryCount = oldEntryCount + additionalEntries;
        if (newEntryCount > 077) {
            throw new RuntimeException("Invalid additional entries value");
        }

        int newTableSizeWords = newEntryCount * entrySize;
        if (tableReferenceOffset == LAST_TABLE_REFERENCE_OFFSET) {
            if (tableOffset + newTableSizeWords > _arraySlice.getSize()) {
                int increment = tableOffset + newTableSizeWords - _arraySlice.getSize();
                expandArray(increment);
            }
        } else {
            shiftTable(tableReferenceOffset + 1, tableOffset + newTableSizeWords);
        }

        int newEntryOffset = tableOffset + oldTableSizeWords;
        for (int ex = oldEntryCount; ex < newEntryCount; ++ex) {
            clearTableEntry(tableOffset, ex);
        }

        setTableEntryCount(tableReferenceOffset, newEntryCount);
        return newEntryOffset;
    }

    /**
     * Finds the offset from the start of the CDB for the given ChannelModule by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findChannelModuleEntry(
        final ChannelModule channelModule
    ) {
        int result = 0;
        long[] nameWords = getNameWords(channelModule._name);
        int entryCount = getTableEntryCount(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Finds the offset from the start of the CDB for the given Device by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findDeviceEntry(
        final Device device
    ) {
        int result = 0;
        long[] nameWords = getNameWords(device._name);
        int entryCount = getTableEntryCount(DEVICE_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(DEVICE_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(DEVICE_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Finds the offset from the start of the CDB for the given InputOutputProcessor by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findInputOutputProcessorEntry(
        final InputOutputProcessor iop
    ) {
        int result = 0;
        long[] nameWords = getNameWords(iop._name);
        int entryCount = getTableEntryCount(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Finds the offset from the start of the CDB for the given InputOutputProcessor by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findInstructionProcessorEntry(
        final InstructionProcessor ip
    ) {
        int result = 0;
        long[] nameWords = getNameWords(ip._name);
        int entryCount = getTableEntryCount(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Finds the offset from the start of the CDB of the mailslot for the given source/destination UPI index combination.
     * @param sourceUPIIndex UPI index of source processor
     * @param destinationUPIIndex UPI index of destination processor
     * @return offset if found, else 0
     */
    protected int findMailSlotEntry(
        final int sourceUPIIndex,
        final int destinationUPIIndex
    ) {
        int entryCount = getTableEntryCount(MAIL_SLOT_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(MAIL_SLOT_TABLE_REFERENCE_OFFSET, ex);
            int entrySourceIndex = getMailSlotEntrySourceUPIIndex(entryOffset);
            int entryDestIndex = getMailSlotEntryDestinationUPIIndex(entryOffset);
            if ((entrySourceIndex == sourceUPIIndex) && (entryDestIndex == destinationUPIIndex)) {
                return entryOffset;
            }
        }

        return 0;
    }

    /**
     * Finds the offset from the start of the CDB for the given MainStorageProcessor by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findMainStorageProcessorEntry(
        final MainStorageProcessor msp
    ) {
        int result = 0;
        long[] nameWords = getNameWords(msp._name);
        int entryCount = getTableEntryCount(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Finds the offset from the start of the CDB for the given InputOutputProcessor by comparing node names.
     * @return offset from start of CDB of entry if found, else 0
     */
    protected int findSystemProcessorEntry(
        final SystemProcessor msp
    ) {
        int result = 0;
        long[] nameWords = getNameWords(msp._name);
        int entryCount = getTableEntryCount(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getTableEntryOffset(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
            if (getNodeEntryState(entryOffset) != NODE_STATE_UNUSED) {
                long[] entryName = getNodeEntryName(entryOffset);
                if (Arrays.equals(nameWords, entryName)) {
                    result = getTableEntryOffset(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, ex);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Converts a name of a varying number of characters to a proper node name
     * stored as uppercase ASCII LJSF in two words.
     */
    protected static long[] getNameWords(
        final String name
    ) {
        String adjustedName = String.format("%-8s", name.toUpperCase());
        return new long[]{
            Word36.stringToWordASCII(adjustedName.substring(0, 4)).getW(),
            Word36.stringToWordASCII(adjustedName.substring(4, 8)).getW()
        };
    }

    /**
     * (Re)Populates an existing entry with detail from the given node.
     * @param channelModule node to be used
     * @param entryOffset location of the entry to be populated
     */
    protected void populateChannelModuleEntry(
        final ChannelModule channelModule,
        final int entryOffset
    ) {
        setNodeEntryState(entryOffset, 1);
        setNodeEntryType(entryOffset, channelModule._channelModuleType.getCode());
        setNodeEntryModel(entryOffset, 0);
        setNodeEntryName(entryOffset, getNameWords(channelModule._name));
        long[] osState = { 0, 0 };
        setNodeEntryOperatingSystemState(entryOffset, osState);

        //  fill in device refs
        for (int devIndex = 0; devIndex < InventoryManager.MAX_DEVICES_PER_CHANNEL_MODULE; ++devIndex) {
            Node descendant = channelModule._descendants.get(devIndex);
            setChannelModuleEntryDeviceConnected(entryOffset, devIndex, false);
            if (descendant != null) {
                Device device = (Device) descendant;
                int deviceOffset = findDeviceEntry(device);
                if (deviceOffset > 0) {
                    setChannelModuleEntryDeviceConnected(entryOffset, devIndex, true);
                    setChannelModuleEntryDeviceReference(entryOffset, devIndex, deviceOffset);
                }
            }
        }

        //  Update containing IOP
        if (channelModule._ancestors.size() != 1) {
            throw new RuntimeException("Problem with ancestors container for " + channelModule._name);
        }
        InputOutputProcessor iop = (InputOutputProcessor) channelModule._ancestors.iterator().next();
        int iopEntryOffset = findInputOutputProcessorEntry(iop);
        if (iopEntryOffset != 0) {
            for (Map.Entry<Integer, Node> entry : iop._descendants.entrySet()) {
                int cmIndex = entry.getKey();
                ChannelModule descendant = (ChannelModule) entry.getValue();
                if (descendant == channelModule) {
                    setInputOutputProcessorEntryChannelModuleConnected(iopEntryOffset, cmIndex, true);
                    setInputOutputProcessorEntryChannelModuleReference(iopEntryOffset, cmIndex, entryOffset);
                    break;
                }
            }
        }
    }

    /**
     * (Re)Populates an existing entry with detail from the given node.
     * @param device node to be used
     * @param entryOffset location of the entry to be populated
     */
    protected void populateDeviceEntry(
        final Device device,
        final int entryOffset
    ) {
        setNodeEntryState(entryOffset, 1);
        setNodeEntryType(entryOffset, device._deviceType.getCode());
        setNodeEntryModel(entryOffset, device._deviceModel.getCode());
        setNodeEntryName(entryOffset, getNameWords(device._name));
        long[] osState = { 0, 0 };
        setNodeEntryOperatingSystemState(entryOffset, osState);

        //  For all ancestor chmods which we already know about,
        //  update that chmod's ref list to include this device.
        for (Node ancestor : device._ancestors) {
            ChannelModule chmod = (ChannelModule) ancestor;
            int chmodEntryOffset = findChannelModuleEntry(chmod);
            if (chmodEntryOffset > 0) {
                for (Map.Entry<Integer, Node> entry : chmod._descendants.entrySet()) {
                    int devIndex = entry.getKey();
                    Device descendant = (Device) entry.getValue();
                    if (descendant == device) {
                        setChannelModuleEntryDeviceConnected(chmodEntryOffset, devIndex, true);
                        setChannelModuleEntryDeviceReference(chmodEntryOffset, devIndex, entryOffset);
                        break;
                    }
                }
            }
        }
    }

    /**
     * (re)Populates an existing IOP entry
     * @param iop represents the processor to be represented in the appropriate table
     * @param entryOffset offset from the start of the CDB of the entry to be populated
     */
    protected void populateInputOutputProcessorEntry(
        final InputOutputProcessor iop,
        final int entryOffset
    ) {
        populateProcessorEntry(iop, entryOffset);

        //  Write channel module references
        for (int cmIndex = 0; cmIndex < InventoryManager.MAX_CHANNEL_MODULES_PER_IOP; ++cmIndex) {
            Node descendant = iop._descendants.get(cmIndex);
            setInputOutputProcessorEntryChannelModuleConnected(entryOffset, cmIndex, false);
            if (descendant != null) {
                ChannelModule chMod = (ChannelModule) descendant;
                int chmodOffset = findChannelModuleEntry(chMod);
                if (chmodOffset > 0) {
                    setInputOutputProcessorEntryChannelModuleConnected(entryOffset, cmIndex, true);
                    setInputOutputProcessorEntryChannelModuleReference(entryOffset, cmIndex, chmodOffset);
                }
            }
        }

        //  Update mail slot table
        int iopUpiIndex = iop._upiIndex;
        int ipEntryCount = getTableEntryCount(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int ipx = 0; ipx < ipEntryCount; ++ipx) {
            int ipEntryOffset = getTableEntryOffset(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, ipx);
            if (ipEntryOffset > 0) {
                int ipUpiIndex = getProcessorEntryUPIIndex(ipEntryOffset);
                establishMailSlot(ipUpiIndex, iopUpiIndex);
            }
        }
    }

    /**
     * (re)Populates an existing IP entry
     * @param ip represents the processor to be represented in the appropriate table
     * @param entryOffset offset from the start of the CDB of the entry to be populated
     */
    protected void populateInstructionProcessorEntry(
        final InstructionProcessor ip,
        final int entryOffset
    ) {
        populateProcessorEntry(ip, entryOffset);

        //  update mail slot table
        int ipUpiIndex = ip._upiIndex;
        int iopEntryCount = getTableEntryCount(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET);
        for (int iopx = 0; iopx < iopEntryCount; ++iopx) {
            int iopEntryOffset = getTableEntryOffset(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, iopx);
            if (iopEntryOffset > 0) {
                int iopUpiIndex = getProcessorEntryUPIIndex(iopEntryOffset);
                establishMailSlot(ipUpiIndex, iopUpiIndex);
            }
        }
    }

    /**
     * (re)Populates an existing MSP entry
     * @param msp represents the processor to be represented in the appropriate table
     * @param entryOffset offset from the start of the CDB of the entry to be populated
     */
    protected void populateMainStorageProcessorEntry(
        final MainStorageProcessor msp,
        final int entryOffset
    ) {
        populateProcessorEntry(msp, entryOffset);
        setMainStorageProcessorEntryFixedSize(entryOffset, msp.getFixedSize());
        setMainStorageProcessorEntryTotalSegments(entryOffset, msp.getMaxSegments());
    }

    /**
     * Common code for populating a processor entry
     */
    protected void populateProcessorEntry(
        final Processor processor,
        final int entryOffset
    ) {
        int state = NODE_STATE_ACTIVE;
        if (processor._isReady) {
            if (processor instanceof InstructionProcessor) {
                InstructionProcessor ip = (InstructionProcessor) processor;
                state = ip.isStopped() ? NODE_STATE_ACTIVE : NODE_STATE_RUNNING;
            }
        }

        setNodeEntryState(entryOffset, state);
        setNodeEntryType(entryOffset, processor._type.getCode());
        setNodeEntryModel(entryOffset, 0);
        setNodeEntryName(entryOffset, getNameWords(processor._name));
        setProcessorEntryUPIIndex(entryOffset, processor._upiIndex);

        long[] osState = { 0, 0 };
        setNodeEntryOperatingSystemState(entryOffset, osState);
    }

    /**
     * (re)Populates an existing SP entry
     * @param sp represents the processor to be represented in the appropriate table
     * @param entryOffset offset from the start of the CDB of the entry to be populated
     */
    protected void populateSystemProcessorEntry(
        final SystemProcessor sp,
        final int entryOffset
    ) {
        populateProcessorEntry(sp, entryOffset);
    }

    /**
     * Shifts a table further up or down within the ConfigDataBank.
     * Before a table is shifted downward, it shifts any following table (ad infinitum, at least to the last table).
     * After a table is shifted upward, it shifts any following table.
     * If the table is already at the indicated point, it is not shifted.
     * @param tableReferenceOffset Offset from the start of the ConfigDataBank of the table reference describing the table to be shifted.
     * @param newTableOffset New offset from the start of the ConfigDataBank, to which the table should be moved.
     */
    protected void shiftTable(
        final int tableReferenceOffset,
        final int newTableOffset
    ) {
        if ((tableReferenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (tableReferenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        int oldTableOffset = getTableOffset(tableReferenceOffset);
        int tableSizeWords = getTableSizeWords(tableReferenceOffset);

        if (newTableOffset < oldTableOffset) {
            //  We are shifting backwards, so we will shift the words in increasing order
            int sx = oldTableOffset;
            int dx = newTableOffset;
            for (int wx = 0; wx < tableSizeWords; ++wx, ++dx, ++sx) {
                _arraySlice.set(dx, _arraySlice.get(sx));
            }

            //  If there's a table after this one, shift it backward as well.
            //  Otherwise, update table usage. We don't ever contract the array slice.
            int tableLimit = newTableOffset + tableSizeWords;
            if (tableReferenceOffset != LAST_TABLE_REFERENCE_OFFSET) {
                setArrayUsed(tableLimit);
            } else {
                shiftTable(tableReferenceOffset + 1, tableLimit);
            }
        } else if (newTableOffset > oldTableOffset) {
            if (tableReferenceOffset == LAST_TABLE_REFERENCE_OFFSET) {
                //  This is the last table in the CDB - if the CDB is not large enough, bigger it.
                if (newTableOffset + tableSizeWords > _arraySlice.getSize()) {
                    int increment = newTableOffset + tableSizeWords - _arraySlice.getSize();
                    expandArray(increment);
                }
            } else {
                //  There's a table behind this one - shift it first
                shiftTable(tableReferenceOffset + 1, newTableOffset + tableSizeWords);
            }

            //  Shift the table (finally)...
            //  we're expanding, so we have to move word by word from the back to the front.
            int sx = oldTableOffset + tableSizeWords;
            int dx = newTableOffset + tableSizeWords;
            while (sx > oldTableOffset) {
                long value = _arraySlice.get(--sx);
                _arraySlice.set(--dx, value);
            }

            setTableOffset(tableReferenceOffset, newTableOffset);
        }
    }

    /**
     * Attempts to lock the TS cell.
     * If it cannot be locked within the timeout, we throw and let the client deal with it.
     */
    protected void testSetLock(
    ) throws StorageLockException {
        long expiry = System.currentTimeMillis() + LOCK_TIMEOUT_MILLIS;
        synchronized (this) {
            while (Word36.getS1(_arraySlice.get(0)) != 0) {
                if (System.currentTimeMillis() > expiry) {
                    throw new StorageLockException();
                }

                try {
                    wait(10);
                } catch (InterruptedException ex) {
                    //  ignore this
                }
            }

            spliceS1(0, 1);
        }
    }

    /**
     * Unlocks the TS cell.
     */
    protected void testSetUnlock() {
        spliceS1(0, 0);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  package-private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Adds a ChannelModule to the ConfigDataBank.
     */
    public void addChannelModule(
        final ChannelModule channelModule
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, 1);
        populateChannelModuleEntry(channelModule, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds one or more ChannelModule nodes to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    public void addChannelModules(
        final List<ChannelModule> channelModules
    ) throws StorageLockException {
        testSetLock();
        if (channelModules.size() > 0) {
            int entryOffset = expandTable(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, channelModules.size());
            for (ChannelModule channelModule : channelModules) {
                populateChannelModuleEntry(channelModule, entryOffset);
                entryOffset += CHANNEL_MODULE_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds a single device entry to the ConfigDataBank.
     */
    public void addDevice(
        final Device device
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(DEVICE_TABLE_REFERENCE_OFFSET, 1);
        populateDeviceEntry(device, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds device entries for the given devices to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    public void addDevices(
        final List<Device> devices
    ) throws StorageLockException {
        testSetLock();
        if (devices.size() > 0) {
            int entryOffset = expandTable(DEVICE_TABLE_REFERENCE_OFFSET, devices.size());
            for (Device device : devices) {
                populateDeviceEntry(device, entryOffset);
                entryOffset += DEVICE_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds an InputOutputProcessor node to the ConfigDataBank.
     * Updates mail slot table as necessary, and populates channel module references
     * for channel modules reflected in the InputOutputProcessor object for which
     * we already have entries in the CDB... the ones for which an entry does not yet exist
     * are silently ignored on the theory that those channel modules will be added
     * fairly soon, at which point the appropriate refs in the IOP entries will be written.
     */
    protected void addInputOutputProcessor(
        final InputOutputProcessor iop
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, 1);
        populateInputOutputProcessorEntry(iop, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds one or more InputOutputProcessor nodes to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    protected void addInputOutputProcessors(
        final List<InputOutputProcessor> iops
    ) throws StorageLockException {
        testSetLock();
        if (iops.size() > 0) {
            int entryOffset = expandTable(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, iops.size());
            for (InputOutputProcessor iop : iops) {
                populateInputOutputProcessorEntry(iop, entryOffset);
                entryOffset += INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds an InstructionProcessor node to the ConfigDataBank.
     * Adds mailslot entries if necessary.
     */
    protected void addInstructionProcessor(
        final InstructionProcessor ip
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, 1);
        populateInstructionProcessorEntry(ip, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds one or more InstructionProcessor nodes to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    protected void addInstructionProcessors(
        final List<InstructionProcessor> ips
    ) throws StorageLockException {
        testSetLock();
        if (ips.size() > 0) {
            int entryOffset = expandTable(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, ips.size());
            for (InstructionProcessor ip : ips) {
                populateInstructionProcessorEntry(ip, entryOffset);
                entryOffset += INSTRUCTION_PROCESSOR_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds a MainStorageProcessor node to the ConfigDataBank.
     * Adds mailslot entries if necessary.
     */
    protected void addMainStorageProcessor(
        final MainStorageProcessor msp
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, 1);
        populateMainStorageProcessorEntry(msp, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds one or more MainStorageProcessor nodes to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    protected void addMainStorageProcessors(
        final List<MainStorageProcessor> msps
    ) throws StorageLockException {
        testSetLock();
        if (msps.size() > 0) {
            int entryOffset = expandTable(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, msps.size());
            for (MainStorageProcessor msp : msps) {
                populateMainStorageProcessorEntry(msp, entryOffset);
                entryOffset += MAIN_STORAGE_PROCESSOR_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds a SystemProcessor node to the ConfigDataBank.
     * Adds mailslot entries if necessary.
     */
    protected void addSystemProcessor(
        final SystemProcessor sp
    ) throws StorageLockException {
        testSetLock();
        int entryOffset = expandTable(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, 1);
        populateSystemProcessorEntry(sp, entryOffset);
        incrementConfigurationNumber();
        testSetUnlock();
    }

    /**
     * Adds one or more SystemProcessor nodes to the ConfigDataBank.
     * Minimizes table shifting and movement.
     */
    protected void addSystemProcessors(
        final List<SystemProcessor> sps
    ) throws StorageLockException {
        testSetLock();
        if (sps.size() > 0) {
            int entryOffset = expandTable(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, sps.size());
            for (SystemProcessor sp : sps) {
                populateSystemProcessorEntry(sp, entryOffset);
                entryOffset += SYSTEM_PROCESSOR_ENTRY_SIZE;
            }
        }
        incrementConfigurationNumber();
        testSetUnlock();
    }

    //TODO deleteChannelModule(ChannelModule)
    //TODO deleteDevice(Device)
    //TODO deleteInputOutputProcessor(InputOutputProcessor)
    //TODO deleteInstructionProcessor(InputOutputProcessor)
    //TODO deleteMainStorageProcessor(InputOutputProcessor)
    //TODO deleteSystemProcessor(InputOutputProcessor)

    /**
     * Clears the CDB and populates it with all of the information currently known to the InventoryManager
     */
    public void populate(
        final InventoryManager im
    ) throws StorageLockException {
        testSetLock();
        clear();
        addSystemProcessors(im.getSystemProcessors());
        addInstructionProcessors(im.getInstructionProcessors());
        addInputOutputProcessors(im.getInputOutputProcessors());
        addMainStorageProcessors(im.getMainStorageProcessors());
        addChannelModules(im.getChannelModules());
        addDevices(im.getDevices());
        incrementConfigurationNumber();
        testSetUnlock();
    }

    //TODO updateChannelModule(ChannelModule)
    //TODO udpateDevice(Device)
    //TODO updateInputOutputProcessor(InputOutputProcessor)
    //TODO updateInstructionProcessor(InputOutputProcessor)
    //TODO updateMainStorageProcessor(InputOutputProcessor)
    //TODO updateSystemProcessor(InputOutputProcessor)
}
