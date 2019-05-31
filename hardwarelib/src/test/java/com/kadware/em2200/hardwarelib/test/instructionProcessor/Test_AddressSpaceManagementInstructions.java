/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.GeneralRegisterSet;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_AddressSpaceManagementInstructions extends BaseFunctions {

    //  No basic version of DABT

    @Test
    public void decelerateActiveBaseTable_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 30",
            "",
            "$(1),START$*",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B2",
            "          HALT      0",
        };

        ActiveBaseTableEntry[] expectedValues = {
            new ActiveBaseTableEntry(0_000004_001000L),
            new ActiveBaseTableEntry(0_000005_002000L),
            new ActiveBaseTableEntry(0_000006_001000L),
            new ActiveBaseTableEntry(0_000007_002000L),
            new ActiveBaseTableEntry(0_000010_001000L),
            new ActiveBaseTableEntry(0_000011_002000L),
            new ActiveBaseTableEntry(0_000000_000000L),
            new ActiveBaseTableEntry(0_201025_022000L),
            new ActiveBaseTableEntry(0_201026_022000L),
            new ActiveBaseTableEntry(0_201027_022000L),
            new ActiveBaseTableEntry(0_201030_022000L),
            new ActiveBaseTableEntry(0_404037_040000L),
            new ActiveBaseTableEntry(0_404777_047777L),
            new ActiveBaseTableEntry(0_720020_050000L),
            new ActiveBaseTableEntry(0_720030_050050L),
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.loadActiveBaseTable(expectedValues);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(1);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(1, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getH1());
        assertEquals(16, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getH2());

        assertArrayEquals(expectedValues, processors._instructionProcessor.getActiveBaseTableEntries());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 30",
            "",
            "$(1),START$*",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B8",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(1);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 30",
            "",
            "$(1),START$*",
            "          LXI,U     X2,1",
            "          LXM,U     X2,150",
            "          DABT      DATA,*X2,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(1);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error3(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 30",
            "",
            "$(1),START$*",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B8",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(2);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUser_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 2",
            "",
            "$(1),START$*",
            "          SBU       B12,DATA",
            "          LXM,U     X2,1",
            "          SBU       B13,DATA,X2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] data = getBank(processors._instructionProcessor, 13);
        assertEquals(0_600004_000000L, data[0]);
        assertEquals(0_600005_000000L, data[1]);
    }

    @Test
    public void storeBaseRegisterUser_basic_errorPP(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 2",
            "",
            "$(1),START$*",
            "          SBU       B12,DATA",
            "          LXM,U     X2,1",
            "          SBU       B13,DATA,X2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUser_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 2",
            "",
            "$(1),START$*",
            "          SBU       B0,DATA,,B2",
            "          LXM,U     X2,1",
            "          SBU       B2,DATA,X2,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] data = getBank(processors._instructionProcessor, 2);
        assertEquals(0_600004_000000L, data[0]);
        assertEquals(0_600005_000000L, data[1]);
    }
}
