/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.GeneralRegisterSet;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    //  TODO we need unit tests for LBE basic goodpath

    //  TODO we need unit tests for LBE extended goodpath

    @Test
    public void loadBankExec_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBE       B31,BDENTRY    . should cause trouble (bad PP)",
            "          HALT      077            . should not get here",
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
    public void loadBankExec_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBE       B31,BDENTRY,,B2 . should cause trouble (bad PP)",
            "          HALT      077             . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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

    //  TODO  LBED basic goodpath

    //  TODO  LBED extended goodpath

    @Test
    public void loadBankExecDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            //TODO this should be 4-word BReg, not BDENTRY
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBED      B31,BDENTRY    . should cause trouble (bad PP)",
            "          HALT      077            . should not get here",
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
    public void loadBankExecDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            //TODO this should be 4-word BReg, not BDENTRY
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBED      B31,BDENTRY,,B2 . should cause trouble (bad PP)",
            "          HALT      077             . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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
    public void loadBankName_basicBank_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      + 0400010,0 . level 4, BDI 010",
            "          $RES 63",
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA",
            "          HALT      0   . not skipped, basic mode bank",
            "          HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  Sets up B20 to reference the same BDT as B13, as the BDT for level 4.
        //  B13 refers to our data bank, which we've formatted as a BDT... sort of.  Close enough.
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        BaseRegister br13 = processors._instructionProcessor.getBaseRegister(13);
        processors._instructionProcessor.setBaseRegister(20, br13);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_400004_000000L, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
    }

    @Test
    public void loadBankName_basicBank_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0400010,0 . level 4, BDI 010",
            "          $RES 63",
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA,,B2",
            "          HALT      0   . not skipped, basic mode bank",
            "          HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  Sets up B20 to reference the same BDT as B2, as the BDT for level 4.
        //  B2 refers to our data bank, which we've formatted as a BDT... sort of.  Close enough.
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        BaseRegister br2 = processors._instructionProcessor.getBaseRegister(2);
        processors._instructionProcessor.setBaseRegister(20, br2);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_400004_000000L, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
    }

    @Test
    public void loadBankName_extendedBank_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      + 0400010,0 . level 4, BDI 010",
            "          $RES 63",
            "BDENTRY   .",
            "          + 0330000,0 . extended mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA",
            "          HALT      077 . skipped, not basic mode bank",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  Sets up B20 to reference the same BDT as B13, as the BDT for level 4.
        //  B13 refers to our data bank, which we've formatted as a BDT... sort of.  Close enough.
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        BaseRegister br13 = processors._instructionProcessor.getBaseRegister(13);
        processors._instructionProcessor.setBaseRegister(20, br13);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_400004_000000L, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
    }

    @Test
    public void loadBankName_extendedBank_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0400010,0 . level 4, BDI 010",
            "          $RES 63",
            "BDENTRY   .",
            "          + 0330000,0 . extended mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA,,B2",
            "          HALT      077 . skipped, not basic mode bank",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  Sets up B20 to reference the same BDT as B2, as the BDT for level 4.
        //  B2 refers to our data bank, which we've formatted as a BDT... sort of.  Close enough.
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        BaseRegister br2 = processors._instructionProcessor.getBaseRegister(2);
        processors._instructionProcessor.setBaseRegister(20, br2);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_400004_000000L, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
    }

    @Test
    public void loadBankName_badPP_basic(
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
            "          LBN       X5,DATA",
            "          HALT      0",
            "          HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(1);
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
    public void loadBankName_addrException_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      + 0507777,0 . non-existent L,BDI",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA",
            "          HALT      077",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01011, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBankName_addrException_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0507777,0 . non-existent L,BDI",
            "",
            "$(1),START$*",
            "          LBN       X5,DATA,,B2",
            "          HALT      077",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(3);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01011, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBankName_reserved_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      + 0,0 . L,BDI in H1 is 0,0",
            "          + 31,0 . L,BDI in H1 is 0,31",
            "",
            "$(1),START$*",
            "          LXI,U     X1,1",
            "          LXM,U     X1,0",
            "          LBN       X5,DATA,*X1",
            "          HALT      077        . skipped",
            "          LBN       X6,DATA,*X1",
            "          HALT      077        . skipped",
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
        assertEquals(0_000001_000002, processors._instructionProcessor.getExecOrUserXRegister(1).getW());
        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(31, processors._instructionProcessor.getExecOrUserXRegister(6).getH1());
        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(6).getH2());
    }

    @Test
    public void loadBankName_reserved_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0,0 . L,BDI in H1 is 0,0",
            "          + 31,0 . L,BDI in H1 is 0,31",
            "",
            "$(1),START$*",
            "          LXI,U     X1,1",
            "          LXM,U     X1,0",
            "          LBN       X5,DATA,*X1,B2",
            "          HALT      077        . skipped",
            "          LBN       X6,DATA,*X1,B2",
            "          HALT      077        . skipped",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000001_000002, processors._instructionProcessor.getExecOrUserXRegister(1).getW());
        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(31, processors._instructionProcessor.getExecOrUserXRegister(6).getH1());
        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(6).getH2());
    }

    //  TODO we need unit tests for LBU basic goodpath

    //  TODO we need unit tests for LBU extended goodpath

    @Test
    public void loadBankUser_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBU       B5,BDENTRY     . should cause trouble (bad PP)",
            "          HALT      077            . should not get here",
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

    //  TODO we need unit tests for LBUD basic goodpath

    //  TODO we need unit tests for LBUD extended goodpath

    @Test
    public void loadBankUserDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            //TODO this should be 4-word BReg, not BDENTRY
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBUD      B9,BDENTRY     . should cause trouble (bad PP)",
            "          HALT      077            . should not get here",
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
    public void loadBankUserDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            //TODO this should be 4-word BReg, not BDENTRY
            "BDENTRY   .",
            "          + 0330100,0 . basic mode bank",
            "          + 0001000,02000",
            "          + 0",
            "          + 0",
            "          + 04,0 . displacement is 4",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1),START$*",
            "          LBUD      B7,BDENTRY,,B2 . should cause trouble (bad PP)",
            "          HALT      077            . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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

    //TODO LBUD B0 fails

    @Test
    public void storeBaseRegisterExecDirect_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8",
            "          SBED      B17,DATA,*X8",
            "          SBED      B18,DATA,*X8",
            "          SBED      B19,DATA,*X8",
            "          SBED      B20,DATA,*X8",
            "          SBED      B21,DATA,*X8",
            "          SBED      B22,DATA,*X8",
            "          SBED      B23,DATA,*X8",
            "          SBED      B24,DATA,*X8",
            "          SBED      B25,DATA,*X8",
            "          SBED      B26,DATA,*X8",
            "          SBED      B27,DATA,*X8",
            "          SBED      B28,DATA,*X8",
            "          SBED      B29,DATA,*X8",
            "          SBED      B30,DATA,*X8",
            "          SBED      B31,DATA,*X8",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  set up some fake banks - 30 and 31 are void banks
        for (int bx = 24; bx < 30; ++bx) {
            BaseRegister br = new BaseRegister(new AbsoluteAddress(processors._mainStorageProcessor.getUPI(), 0, bx * 1024),
                                               false,
                                               bx * 512,
                                               bx * 512 + 511,
                                               new AccessInfo(0, bx),
                                               new AccessPermissions(false, true, true),
                                               new AccessPermissions(false, true, true));
            processors._instructionProcessor.setBaseRegister(bx, br);
        }

        BaseRegister br30 = new BaseRegister();
        processors._instructionProcessor.setBaseRegister(30, br30);
        BaseRegister br31 = new BaseRegister(new AbsoluteAddress((short) 0, 0, 0),
                                             false,
                                             02000,
                                             01777,
                                             new AccessInfo(0, 0),
                                             new AccessPermissions(false, false, false),
                                             new AccessPermissions(false, false, false));
        processors._instructionProcessor.setBaseRegister(31, br31);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        //  create BaseRegister objects for all the data that has been created via SBED
        assertEquals(0_000004_000100, processors._instructionProcessor.getExecOrUserXRegister(8).getW());
        BaseRegister[] baseRegisters = new BaseRegister[32];
        long[] data = getBank(processors._instructionProcessor, 13);
        for (int bx = 16, dx = 0; bx < 32; ++bx, dx += 4) {
            long[] subData = new long[4];
            subData[0] = data[dx];
            subData[1] = data[dx + 1];
            subData[2] = data[dx + 2];
            subData[3] = data[dx + 3];
            baseRegisters[bx] = new BaseRegister(subData);
        }

        for (int bx = 16; bx < 32; ++bx) {
            assertEquals(processors._instructionProcessor.getBaseRegister(bx), baseRegisters[bx]);
        }

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
    }

    @Test
    public void storeBaseRegisterExecDirect_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8,B2",
            "          SBED      B17,DATA,*X8,B2",
            "          SBED      B18,DATA,*X8,B2",
            "          SBED      B19,DATA,*X8,B2",
            "          SBED      B20,DATA,*X8,B2",
            "          SBED      B21,DATA,*X8,B2",
            "          SBED      B22,DATA,*X8,B2",
            "          SBED      B23,DATA,*X8,B2",
            "          SBED      B24,DATA,*X8,B2",
            "          SBED      B25,DATA,*X8,B2",
            "          SBED      B26,DATA,*X8,B2",
            "          SBED      B27,DATA,*X8,B2",
            "          SBED      B28,DATA,*X8,B2",
            "          SBED      B29,DATA,*X8,B2",
            "          SBED      B30,DATA,*X8,B2",
            "          SBED      B31,DATA,*X8,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  set up some fake banks - 30 and 31 are void banks
        for (int bx = 24; bx < 30; ++bx) {
            BaseRegister br = new BaseRegister(new AbsoluteAddress(processors._mainStorageProcessor.getUPI(), 0, bx * 1024),
                                               false,
                                               bx * 512,
                                               bx * 512 + 511,
                                               new AccessInfo(0, bx),
                                               new AccessPermissions(false, true, true),
                                               new AccessPermissions(false, true, true));
            processors._instructionProcessor.setBaseRegister(bx, br);
        }

        BaseRegister br30 = new BaseRegister();
        processors._instructionProcessor.setBaseRegister(30, br30);
        BaseRegister br31 = new BaseRegister(new AbsoluteAddress((short) 0, 0, 0),
                                             false,
                                             02000,
                                             01777,
                                             new AccessInfo(0, 0),
                                             new AccessPermissions(false, false, false),
                                             new AccessPermissions(false, false, false));
        processors._instructionProcessor.setBaseRegister(31, br31);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0_000004_000100, processors._instructionProcessor.getExecOrUserXRegister(8).getW());
        BaseRegister[] baseRegisters = new BaseRegister[32];
        long[] data = getBank(processors._instructionProcessor, 2);
        for (int bx = 16, dx = 0; bx < 32; ++bx, dx += 4) {
            long[] subData = new long[4];
            subData[0] = data[dx];
            subData[1] = data[dx + 1];
            subData[2] = data[dx + 2];
            subData[3] = data[dx + 3];
            baseRegisters[bx] = new BaseRegister(subData);
        }

        for (int bx = 16; bx < 32; ++bx) {
            assertEquals(processors._instructionProcessor.getBaseRegister(bx), baseRegisters[bx]);
        }

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
    }

    @Test
    public void storeBaseRegisterExecDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8",
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
    public void storeBaseRegisterExecDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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

    @Test
    public void storeBaseRegisterUserDirect_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B0,DATA,*X8",
            "          SBUD      B1,DATA,*X8",
            "          SBUD      B2,DATA,*X8",
            "          SBUD      B3,DATA,*X8",
            "          SBUD      B4,DATA,*X8",
            "          SBUD      B5,DATA,*X8",
            "          SBUD      B6,DATA,*X8",
            "          SBUD      B7,DATA,*X8",
            "          SBUD      B8,DATA,*X8",
            "          SBUD      B9,DATA,*X8",
            "          SBUD      B10,DATA,*X8",
            "          SBUD      B11,DATA,*X8",
            "          SBUD      B12,DATA,*X8",
            "          SBUD      B13,DATA,*X8",
            "          SBUD      B14,DATA,*X8",
            "          SBUD      B15,DATA,*X8",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  set up some fake banks - leave 12 and 13 alone
        for (int bx = 0; bx < 16; ++bx) {
            if ((bx < 12) || (bx > 13)) {
                BaseRegister br = new BaseRegister(new AbsoluteAddress(processors._mainStorageProcessor.getUPI(), 0, bx * 1024),
                                                   false,
                                                   bx * 512,
                                                   bx * 512 + 511,
                                                   new AccessInfo(0, bx),
                                                   new AccessPermissions(false, true, true),
                                                   new AccessPermissions(false, true, true));
                processors._instructionProcessor.setBaseRegister(bx, br);
            }
        }

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0_000004_000100, processors._instructionProcessor.getExecOrUserXRegister(8).getW());
        BaseRegister[] baseRegisters = new BaseRegister[16];
        long[] data = getBank(processors._instructionProcessor, 13);
        for (int bx = 0, dx = 0; bx < 16; ++bx, dx += 4) {
            long[] subData = new long[4];
            subData[0] = data[dx];
            subData[1] = data[dx + 1];
            subData[2] = data[dx + 2];
            subData[3] = data[dx + 3];
            baseRegisters[bx] = new BaseRegister(subData);
        }

        for (int bx = 0; bx < 16; ++bx) {
            assertEquals(processors._instructionProcessor.getBaseRegister(bx), baseRegisters[bx]);
        }

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
    }

    @Test
    public void storeBaseRegisterUserDirect_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B0,DATA,*X8,B2",
            "          SBUD      B1,DATA,*X8,B2",
            "          SBUD      B2,DATA,*X8,B2",
            "          SBUD      B3,DATA,*X8,B2",
            "          SBUD      B4,DATA,*X8,B2",
            "          SBUD      B5,DATA,*X8,B2",
            "          SBUD      B6,DATA,*X8,B2",
            "          SBUD      B7,DATA,*X8,B2",
            "          SBUD      B8,DATA,*X8,B2",
            "          SBUD      B9,DATA,*X8,B2",
            "          SBUD      B10,DATA,*X8,B2",
            "          SBUD      B11,DATA,*X8,B2",
            "          SBUD      B12,DATA,*X8,B2",
            "          SBUD      B13,DATA,*X8,B2",
            "          SBUD      B14,DATA,*X8,B2",
            "          SBUD      B15,DATA,*X8,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        //  set up some fake banks - leave 0 through 2 alone
        for (int bx = 0; bx < 16; ++bx) {
            if (bx > 2) {
                BaseRegister br = new BaseRegister(new AbsoluteAddress(processors._mainStorageProcessor.getUPI(), 0, bx * 1024),
                                                   false,
                                                   bx * 512,
                                                   bx * 512 + 511,
                                                   new AccessInfo(0, bx),
                                                   new AccessPermissions(false, true, true),
                                                   new AccessPermissions(false, true, true));
                processors._instructionProcessor.setBaseRegister(bx, br);
            }
        }

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0_000004_000100, processors._instructionProcessor.getExecOrUserXRegister(8).getW());
        BaseRegister[] baseRegisters = new BaseRegister[16];
        long[] data = getBank(processors._instructionProcessor, 2);
        for (int bx = 0, dx = 0; bx < 16; ++bx, dx += 4) {
            long[] subData = new long[4];
            subData[0] = data[dx];
            subData[1] = data[dx + 1];
            subData[2] = data[dx + 2];
            subData[3] = data[dx + 3];
            baseRegisters[bx] = new BaseRegister(subData);
        }

        for (int bx = 0; bx < 16; ++bx) {
            assertEquals(processors._instructionProcessor.getBaseRegister(bx), baseRegisters[bx]);
        }

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
    }

    @Test
    public void storeBaseRegisterUserDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B0,DATA,*X8",
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
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUserDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      $RES 64",
            "",
            "$(1),START$*",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B8,DATA,*X8,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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
}
