/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.minalib.AbsoluteModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_SystemControlInstructions extends BaseFunctions {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  SYSC general stuff
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void sysc_addSubfunction(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),PACKET",
            "          + 0770000000000",
            "          $RES 7",
            "",
            "$(1),START$*",
            "          SYSC      PACKET,,B2",
            "          HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(526, processors._instructionProcessor.getLatestStopDetail());
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  SYSC memory management
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Subfunction 020: Create dynamic memory block
    //      U+0,S1:     020
    //      U+0,S2:     Upon completion, this will contain
    //                      00: operation completed successfully
    //                      01: given UPI does not correspond to an MSP
    //                      03: requested block length is invalid
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Newly-assigned segment index if status is zero
    //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)

    @Test
    public void sysc_malloc_good(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),PACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      PACKET,,B2",
            "          TZ,S2     PACKET,,B2",
            "          HALT      077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_malloc_badUPI(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),PACKET",
            "          + 0200077000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      PACKET,,B2",
            "          LA,U      A5,1",
            "          TE,S2     A5,PACKET,,B2",
            "          HALT      077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void sysc_malloc_badSize(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),PACKET",
            "          + 0200001000000",
            "          + 0",
            "          - 1",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      PACKET,,B2",
            "          LA,U      A5,3",
            "          TE,S2     A5,PACKET,,B2",
            "          HALT      077",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  Subfunction 021: Release dynamic memory block
    //      U+0,S1:     021
    //      U+0,S2:     Upon completion, this will contain
    //                      00: operation completed successfully
    //                      01: given UPI does not correspond to an MSP
    //                      02: given segment index is not assigned by the MSP
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Segment index of block to be released

    @Test (expected = AddressingExceptionInterrupt.class)
    public void sysc_free_good(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "FREEPACKET",
            "          + 0210001000000",
            "          $RES 7",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          SA        A5,FREEPACKET+1,,B2",
            "          SYSC      FREEPACKET,,B2",
            "          TZ,S2     FREEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
    }

    @Test
    public void sysc_free_badUPI(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "FREEPACKET",
            "          + 0210077000000",
            "          $RES 7",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          SA        A5,FREEPACKET+1,,B2",
            "          SYSC      FREEPACKET,,B2",
            "          LA,U      A5,1",
            "          TE,S2     A5,FREEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
    }

    @Test
    public void sysc_free_badSegment(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "FREEPACKET",
            "          + 0210001000000",
            "          $RES 7",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          LSSL      A5,1",
            "          SA        A5,FREEPACKET+1,,B2",
            "          SYSC      FREEPACKET,,B2",
            "          LA,U      A5,2",
            "          TE,S2     A5,FREEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
    }

    //  Subfunction 022: Resize dynamic memory block
    //      U+0,S1:     022
    //      U+0,S2:     Upon completion, this will contain
    //                      00: operation completed successfully
    //                      01: given UPI does not correspond to an MSP
    //                      02: given segment index is not assigned by the MSP
    //                      03: requested block length is invalid
    //      U+0,S3:     UPI of target MSP
    //      U+1,W:      Segment index of block to be resized
    //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)

    @Test
    public void sysc_realloc_good(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "RESIZEPACKET",
            "          + 0220001000000",
            "          + 0",
            "          + 65536",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          SA        A5,RESIZEPACKET+1,,B2",
            "          SYSC      RESIZEPACKET,,B2",
            "          TZ,S2     RESIZEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
        assertEquals(65536, slice._length);
    }

    @Test
    public void sysc_realloc_badUPI(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "RESIZEPACKET",
            "          + 0220077000000",
            "          + 0",
            "          + 65536",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          SA        A5,RESIZEPACKET+1,,B2",
            "          SYSC      RESIZEPACKET,,B2",
            "          LA,U      A3,1",
            "          TE,S2     A3,RESIZEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_realloc_badSegment(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "RESIZEPACKET",
            "          + 0220001000000",
            "          + 0",
            "          + 65536",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          LSSL      A5,2",
            "          SA        A5,RESIZEPACKET+1,,B2",
            "          SYSC      RESIZEPACKET,,B2",
            "          LA,U      A3,2",
            "          TE,S2     A3,RESIZEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }

    @Test
    public void sysc_realloc_badSize(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      .",
            "ALLOCPACKET",
            "          + 0200001000000",
            "          + 0",
            "          + 32768",
            "          $RES 5",
            "",
            "RESIZEPACKET",
            "          + 0220001000000",
            "          + 0",
            "          - 1",
            "          $RES 5",
            "",
            "$(1),START$*",
            "          SYSC      ALLOCPACKET,,B2",
            "          TZ,S2     ALLOCPACKET,,B2",
            "          HALT      077",
            ".",
            "          LA        A5,ALLOCPACKET+1,,B2",
            "          SA        A5,RESIZEPACKET+1,,B2",
            "          SYSC      RESIZEPACKET,,B2",
            "          LA,U      A3,3",
            "          TE,S2     A3,RESIZEPACKET,,B2",
            "          HALT      076",
            ".",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bank = getBank(processors._instructionProcessor, 2);
        int segment = (int) bank[1];
        assertEquals(1, segment);

        ArraySlice slice = processors._mainStorageProcessor.getStorage(segment);
        assertEquals(32768, slice._length);
    }
}
