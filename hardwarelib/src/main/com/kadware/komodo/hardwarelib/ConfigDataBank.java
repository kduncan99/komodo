/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.NotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains the configuration data bank.
 *
 * The content consists of a number of 36-bit words wrapped in longs.
 * The format is as follows:
 *
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                                |       CDB size in words        |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                                |   current CDB usage in words   |
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
 *     + 8      |# of disk |                     |        offset of first         |
 *              |  device  |                     |       disk device entry        |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 9      |# of tape |                     |        offset of first         |
 *              |  device  |                     |       tape device entry        |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 10     |# of symb |                     |        offset of first         |
 *              |  device  |                     |     symbiont device entry      |
 *              | entries  |                     |                                |
 *              +----------+----------+----------+----------+----------+----------+
 *                                             . . .
 *              +----------+----------+----------+----------+----------+----------+
 *     + 31     |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *
 *  Format of a mail slot entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |  source UPI index   |          |  state   |   dest UPI index    |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                              mail                               |
 *     + 2      |                              slot                               |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused
 *              01 = contains a valid mail slot
 *
 *      It is likely that we only need IPs for sources and IOPs for destinations
 *
 *
 *  Format of an SP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |     UPI index       |
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
 *      type:   01 = emulated SP (the only type)
 *
 *
 *  Format of an IP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |     UPI index       |
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
 *              02 = active
 *
 *      type:   01 = emulated IP (the only type)
 *
 *
 *  Format of an IOP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |     UPI index       |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                            IOP name                             |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating System state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   01 = emulated IOP (the only type)
 *
 *
 *  Format of an MSP entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |     UPI index       |
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
 *      type:   01 = emulated MSP (the only type)
 *
 *
 *  Format of a Channel Module entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |          |  chmod   |
 *     + 0      |                     |          |          |          |  index   |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                       Channel Module name                       |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 5      |                                |  offset from start of CDB of   |
 *              |                                |     containing IOP entry       |
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
 *  Format of a Disk Device entry:
 *              +----------+----------+----------+----------+----------+----------+
 *     + 0      |                     |   type   |  state   |          |          |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 1      |                           Device name                           |
 *     + 2      |                     6 characters ASCII LJSF                     |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 3      |               Operating system state information                |
 *     + 4      |                                                                 |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 5      |                     |  device  |       offset of ancestor       |
 *              |                     |  index   |      channel module entry      |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 6      |                     |  device  |       offset of ancestor       |
 *              |                     |  index   |      channel module entry      |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 7      |                     |  device  |       offset of ancestor       |
 *              |                     |  index   |      channel module entry      |
 *              +----------+----------+----------+----------+----------+----------+
 *     + 8      |                     |  device  |       offset of ancestor       |
 *              |                     |  index   |      channel module entry      |
 *              +----------+----------+----------+----------+----------+----------+
 *
 *      state:  00 = unused (remainder of entry is n/a)
 *              01 = inactive (should never be the case)
 *              02 = active
 *
 *      type:   (see DiskDevice.java for the corresponding enum)
 *
 *
 * Notes:
 *      All names are 6 alphanumeric characters max, stored in ASCII, LJSF,
 *          in two words. H2 of the second word will contain spaces.
 *      All unused fields are reserved and will be set to zero
 *          by whichever entity populates the CDB.
 *
 *  This gets populated by SP at IPL time, in one shot.
 *  It is encapsulated in the Processor base class for the convenience of the various processor objects.
 *  This bank is built by the SP just prior to ipl().
 *  It is always located at UPI:0 seg:0 offset:0, and may or may not be a fixed size.
 *  For the time being, it is expected to be static during the lifetime of the operating system.
 *  Any configuration change will be done between OS sessions, and may require a reconfiguration boot (or not).
 *  However, it is possible that the SP could dynamic this in one of two ways:
 *      lock access to the bank in order to modify it, including moving tables around if/as necessary
 *      create the bank with the (or a) max number of entries of all types, to avoid moving tables around,
 *          and then fill in unused entries or create unused entries as necessary.
 *  Either method would require that the running OS be able to detect that the configuration has changed.
 *  Possibly a monotonically-increasing configuration number in the header block?
 */
@SuppressWarnings("DuplicatedCode")
public class ConfigDataBank {

    private static final int DEFAULT_BANK_SIZE = 32 * 1024; //  32k words
    private static final int HEADER_SIZE = 32;

    private static final int FIRST_TABLE_REFERENCE_OFFSET = 2;
    private static final int MAIL_SLOT_TABLE_REFERENCE_OFFSET = 2;
    private static final int MAIL_SLOT_ENTRY_SIZE = 3;
    private static final int SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET = 3;
    private static final int SYSTEM_PROCESSOR_ENTRY_SIZE = 5;
    private static final int INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET = 4;
    private static final int INSTRUCTION_PROCESSOR_ENTRY_SIZE = 5;
    private static final int INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET = 5;
    private static final int INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE = 5;
    private static final int MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET = 6;
    private static final int MAIN_STORAGE_PROCESSOR_ENTRY_SIZE = 7;
    private static final int CHANNEL_MODULE_TABLE_REFERENCE_OFFSET = 7;
    private static final int CHANNEL_MODULE_ENTRY_SIZE = 6;
    private static final int DISK_DEVICE_TABLE_REFERENCE_OFFSET = 8;
    private static final int DISK_DEVICE_ENTRY_SIZE = 5;    //TODO is this correct?
    private static final int TAPE_DEVICE_TABLE_REFERENCE_OFFSET = 9;
    private static final int TAPE_DEVICE_ENTRY_SIZE = 5;    //TODO is this correct?
    private static final int SYMBIONT_DEVICE_TABLE_REFERENCE_OFFSET = 10;
    private static final int SYMBIONT_DEVICE_ENTRY_SIZE = 5;    //TODO is this correct?
    private static final int LAST_TABLE_REFERENCE_OFFSET = 10;

    private static final Map<Integer, Integer> _referenceOffsetToEntrySize = new HashMap<>();
    static {
        _referenceOffsetToEntrySize.put(MAIL_SLOT_TABLE_REFERENCE_OFFSET, MAIL_SLOT_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, SYSTEM_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET, INSTRUCTION_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET, INPUT_OUTPUT_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, MAIN_STORAGE_PROCESSOR_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, CHANNEL_MODULE_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(DISK_DEVICE_TABLE_REFERENCE_OFFSET, DISK_DEVICE_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(TAPE_DEVICE_TABLE_REFERENCE_OFFSET, TAPE_DEVICE_ENTRY_SIZE);
        _referenceOffsetToEntrySize.put(SYMBIONT_DEVICE_TABLE_REFERENCE_OFFSET, SYMBIONT_DEVICE_ENTRY_SIZE);
    }

    private static final ConfigDataBank _instance = new ConfigDataBank();

    private ArraySlice _arraySlice = null;

    ConfigDataBank() {
        _arraySlice = new ArraySlice(new long[DEFAULT_BANK_SIZE]);
        _arraySlice.set(0, Word36.setH2(0, DEFAULT_BANK_SIZE));
        _arraySlice.set(1, Word36.setH2(0, HEADER_SIZE));
        for (int refOffset = FIRST_TABLE_REFERENCE_OFFSET; refOffset <= LAST_TABLE_REFERENCE_OFFSET; ++refOffset) {
            _arraySlice.set(refOffset, Word36.setH2(0, HEADER_SIZE));
        }
    }

    static ConfigDataBank getInstance() {
        return _instance;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Expands the underlying array slice by at least the increment value, modulo 1k.
     */
    void expandArrayByAtLeast(
        final int increment
    ) {
        int newSize = _arraySlice.getSize() + increment;
        newSize = (newSize & 0377) + 1;
        _arraySlice = _arraySlice.copyOf(_arraySlice.getSize() + newSize);
    }

    /**
     * Expands the table indicated by the reference offset by one or more entries
     * and sets the state for the new entries to zero (meaning unused)
     * @param referenceOffset Offset from the start of the ConfigDataBank of the table reference
     *                       describing the table to be expanded.
     * @param additionalEntries number of new entries to be created
     * @return offset from the start of the Config Data Bank, of the first new entry
     */
    int expandTable(
        final int referenceOffset,
        final int additionalEntries
    ) {
        if ((referenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (referenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        if (additionalEntries < 1) {
            throw new RuntimeException("Invalid additional entries value");
        }

        int entrySize = _referenceOffsetToEntrySize.get(referenceOffset);
        long ref = _arraySlice.get(referenceOffset);
        int currentEntryCount = (int) Word36.getS1(ref);
        int tableOffset = (int) Word36.getH2(ref);
        int currentTableSizeWords = currentEntryCount * entrySize;
        int newEntryCount = currentEntryCount + additionalEntries;
        if (newEntryCount > 077) {
            throw new RuntimeException("Invalid additional entries value");
        }

        int newTableSizeWords = newEntryCount * entrySize;
        if (referenceOffset == LAST_TABLE_REFERENCE_OFFSET) {
            if (tableOffset + newTableSizeWords > _arraySlice.getSize()) {
                int increment = tableOffset + newTableSizeWords - _arraySlice.getSize();
                expandArrayByAtLeast(increment);
            }
        } else {
            shiftTable(referenceOffset + 1, tableOffset + newTableSizeWords);
        }

        int newEntryOffset = tableOffset + currentTableSizeWords;
        int entryOffset = newEntryOffset;
        int ex = 0;
        while (ex < additionalEntries) {
            for (int ey = 0; ey < entrySize; ++ey) {
                _arraySlice.set(entryOffset + ey, 0);
            }
            ex++;
            entryOffset += entrySize;
        }

        _arraySlice.set(referenceOffset, Word36.setS1(ref, newEntryCount));
        return newEntryOffset;
    }

    private int findEntryOffsetForProcessor(
        final int upiIndex
    ) throws NotFoundException {
        int refOffset = getRefOffsetForUPIIndex(upiIndex);
        int entryCount = getTableSize(refOffset);
        for (int ex = 0; ex < entryCount; ++ex) {
            int entryOffset = getEntryOffset(refOffset, ex);
            int upiCheck = (int) Word36.getT2(_arraySlice.get(entryOffset));
            if (upiIndex == upiCheck) {
                return entryOffset;
            }
        }

        throw new NotFoundException();
    }

    /**
     * Retrieves the offset of the nth entry of a particular table
     * @param referenceOffset Offset from the start of the ConfigDataBank of the table reference describing the table of interest.
     * @param entryIndex index of the entry of interest
     * @return offset of the requested entry, from the start of the Config Data Bank
     */
    private int getEntryOffset(
        final int referenceOffset,
        final int entryIndex
    ) {
        int tableOffset = getTableOffset(referenceOffset);
        int entryCount = getTableSize(referenceOffset);

        if (entryIndex >= entryCount) {
            throw new RuntimeException("Invalid entry index");
        }

        return tableOffset + entryIndex * _referenceOffsetToEntrySize.get(referenceOffset);
    }

    /**
     * Retrieves the offset from the start of the CDB of the table reference word for the table
     * which contains (or should contain) the given UPI index
     */
    private int getRefOffsetForUPIIndex(
        final int upiIndex
    ) {
        if ((upiIndex >= InventoryManager.FIRST_SP_UPI_INDEX)
            && (upiIndex <= InventoryManager.LAST_SP_UPI_INDEX)) {
            return SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET;
        } else if ((upiIndex >= InventoryManager.FIRST_MSP_UPI_INDEX)
            && (upiIndex <= InventoryManager.LAST_MSP_UPI_INDEX)) {
            return MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET;
        } else if ((upiIndex >= InventoryManager.FIRST_IP_UPI_INDEX)
                   && (upiIndex <= InventoryManager.LAST_IP_UPI_INDEX)) {
            return INSTRUCTION_PROCESSOR_TABLE_REFERENCE_OFFSET;
        } else if ((upiIndex >= InventoryManager.FIRST_IOP_UPI_INDEX)
                   && (upiIndex <= InventoryManager.LAST_IOP_UPI_INDEX)) {
            return INPUT_OUTPUT_PROCESSOR_TABLE_REFERENCE_OFFSET;
        } else {
            throw new RuntimeException("Invalid upiIndex specified");
        }
    }

    /**
     * Retrieves the offset from the start of the CDB of the table identified by the reference
     * @param referenceOffset Offset from the start of the ConfigDataBank of the table reference describing the table of interest.
     */
    private int getTableOffset(
        final int referenceOffset
    ) {
        if ((referenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (referenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        return (int) Word36.getH2(_arraySlice.get(referenceOffset));
    }

    /**
     * Retrieves the number of entries in the table identified by the reference
     * @param referenceOffset Offset from the start of the ConfigDataBank of the table reference describing the table of interest.
     */
    private int getTableSize(
        final int referenceOffset
    ) {
        if ((referenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (referenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        return (int) Word36.getS1(_arraySlice.get(referenceOffset));
    }

    /**
     * Adds one or more ChannelModule nodes to the ConfigDataBank
     */
    private void populate(
        final ChannelModule[] channelModules
    ) {
        try {
            int entryOffset = expandTable(CHANNEL_MODULE_TABLE_REFERENCE_OFFSET, channelModules.length);
            for (ChannelModule channelModule : channelModules) {
                Iterator<Node> iter = channelModule._ancestors.iterator();
                if (!iter.hasNext()) {
                    throw new RuntimeException("Channel Module has no ancestor");
                }

                InputOutputProcessor iop = (InputOutputProcessor) iter.next();
                int cmIndex = 0;
                for (Map.Entry<Integer, Node> entry : iop._descendants.entrySet()) {
                    if (entry.getValue() == iop) {
                        cmIndex = entry.getKey();
                        break;
                    }
                }

                int iopOffset = findEntryOffsetForProcessor(iop._upiIndex);

                long value = Word36.setS3(0, 1);
                value = Word36.setS4(value, 01);
                value = Word36.setS6(value, cmIndex);
                _arraySlice.set(entryOffset, value);

                String adjustedName = String.format("%-8s", channelModule._name.toUpperCase());
                _arraySlice.set(entryOffset + 1, Word36.stringToWordASCII(adjustedName.substring(0, 4)).getW());
                _arraySlice.set(entryOffset + 2, Word36.stringToWordASCII(adjustedName.substring(4, 8)).getW());
                _arraySlice.set(entryOffset + 3, 0);
                _arraySlice.set(entryOffset + 4, 0);
                _arraySlice.set(entryOffset + 5, Word36.setH2(0, iopOffset));
                entryOffset += CHANNEL_MODULE_ENTRY_SIZE;
            }
        } catch (NotFoundException ex) {
            throw new RuntimeException("Problem with Channel Module ancestor");
        }
    }

    private void populate(
        final DiskDevice[] devices
    ) {
        //TODO add entries
    }

    private void populate(
        final InputOutputProcessor[] processors
    ) {
        //TODO add mailslots from all configud IPs to this IOP
        //TODO add entries
    }

    private void populate(
        final InstructionProcessor[] processors
    ) {
        //TODO add mailslots from this IP to all configured IOPs
        //TODO add entries
    }

    /**
     * Adds one or more MainStorageProcessor nodes to the ConfigDataBank
     */
    private void populate(
        final MainStorageProcessor[] processors
    ) {
        int entryOffset = expandTable(MAIN_STORAGE_PROCESSOR_TABLE_REFERENCE_OFFSET, processors.length);
        for (MainStorageProcessor processor : processors) {
            long value = Word36.setS3(0, 1);
            value = Word36.setS4(value, 01);
            value = Word36.setT3(value, processor._upiIndex);
            _arraySlice.set(entryOffset, value);

            String adjustedName = String.format("%-8s", processor._name.toUpperCase());
            _arraySlice.set(entryOffset + 1, Word36.stringToWordASCII(adjustedName.substring(0, 4)).getW());
            _arraySlice.set(entryOffset + 2, Word36.stringToWordASCII(adjustedName.substring(4, 8)).getW());
            _arraySlice.set(entryOffset + 3, 0);
            _arraySlice.set(entryOffset + 4, 0);
            _arraySlice.set(entryOffset + 5, processor.getFixedSize());
            _arraySlice.set(entryOffset + 6, processor.getMaxSegments() - 1);
            entryOffset += MAIN_STORAGE_PROCESSOR_ENTRY_SIZE;
        }
    }

    private void populate(
        final Device[] devices  //  TODO make this SymbiontDevice
    ) {
        //TODO add entries
    }

    /**
     * Adds one or more SystemProcessor nodes to the ConfigDataBank
     */
    private void populate(
        final SystemProcessor[] processors
    ) {
        int entryOffset = expandTable(SYSTEM_PROCESSOR_TABLE_REFERENCE_OFFSET, processors.length);
        for (SystemProcessor processor : processors) {
            long value = Word36.setS3(0, 1);
            value = Word36.setS4(value, 01);
            value = Word36.setT3(value, processor._upiIndex);
            _arraySlice.set(entryOffset, value);

            String adjustedName = String.format("%-8s", processor._name.toUpperCase());
            _arraySlice.set(entryOffset + 1, Word36.stringToWordASCII(adjustedName.substring(0, 4)).getW());
            _arraySlice.set(entryOffset + 2, Word36.stringToWordASCII(adjustedName.substring(4, 8)).getW());
            _arraySlice.set(entryOffset + 3, 0);
            _arraySlice.set(entryOffset + 4, 0);
            entryOffset += SYSTEM_PROCESSOR_ENTRY_SIZE;
        }
    }

    private void populate(
        final TapeDevice[] devices
    ) {
        //TODO add entries
    }

    /**
     * Shifts a table further down in the ConfigDataBank if possible.
     * Before a table is shifted, it shifts any following table (ad infinitum, at least to the last table).
     * @param referenceOffset Offset from the start of the ConfigDataBank of the table reference describing the table to be shifted.
     * @param newTableOffset New offset from the start of the ConfigDataBank, to which the table should be moved.
     *                       If the table is already at or beyond that point, it is not shifted.
     */
    private void shiftTable(
        final int referenceOffset,
        final int newTableOffset
    ) {
        if ((referenceOffset < FIRST_TABLE_REFERENCE_OFFSET)
            || (referenceOffset > LAST_TABLE_REFERENCE_OFFSET)) {
            throw new RuntimeException("Invalid reference offset");
        }

        long ref = _arraySlice.get(referenceOffset);
        int entryCount = (int) Word36.getS1(ref);
        int currentTableOffset = (int) Word36.getH2(ref);
        if (currentTableOffset < newTableOffset) {
            int tableSizeWords = entryCount * _referenceOffsetToEntrySize.get(referenceOffset);
            if (referenceOffset == LAST_TABLE_REFERENCE_OFFSET) {
                if (newTableOffset + tableSizeWords > _arraySlice.getSize()) {
                    int increment = newTableOffset + tableSizeWords - _arraySlice.getSize();
                    expandArrayByAtLeast(increment);
                }

                _arraySlice.set(0, Word36.setH2(_arraySlice.get(0), _arraySlice.getSize()));
            } else {
                shiftTable(referenceOffset + 1, newTableOffset + tableSizeWords);
            }

            //  shift the table (finally)
            //  we're expanding, so we have to move word by word from the back to the front
            int sx = currentTableOffset + tableSizeWords;
            int dx = newTableOffset + tableSizeWords;
            while (sx > currentTableOffset) {
                long value = _arraySlice.get(--sx);
                _arraySlice.set(--dx, value);
            }

            //  Update the reference, and we're done
            _arraySlice.set(referenceOffset, Word36.setH2(ref, newTableOffset));
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  package-private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    void populate(
        final SystemProcessor[] systemProcessors,
        final MainStorageProcessor[] mainStorageProcessors,
        final InstructionProcessor[] instructionProcessors,
        final InputOutputProcessor[] inputOutputProcessors,
        final ChannelModule[] channelModules,
        final DiskDevice[] diskDevices,
        final TapeDevice[] tapeDevices,
        final Device[] symbiontDevices  //  TODO make this SymbiontDevice[] once we have those
    ) {
        populate(systemProcessors);
        populate(instructionProcessors);
        populate(inputOutputProcessors);
        populate(mainStorageProcessors);
        populate(channelModules);
        populate(diskDevices);
        populate(tapeDevices);
        populate(symbiontDevices);
    }
}
