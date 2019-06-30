/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.*;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_AddressSpaceManagementInstructions extends BaseFunctions {

    //  No basic mode version of DABT

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
    public void loadBaseRegisterExec_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600006,0 . 16 words of data",
            "BDENTRY2  + 0600007,0 . void",
            "BDENTRY3  + 0,0       . void",
            "",
            "$(2)      . void bank data, will be BDI 07",
            "$(3)      . useful bank data, will be BDI 06",
            "          $res 16",
            "",
            "$(1),START$*",
            "          LBE       B27,BDENTRY1",
            "          LBE       B28,BDENTRY2",
            "          LBE       B29,BDENTRY2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        BaseRegister br27 = processors._instructionProcessor.getBaseRegister(27);
        assertFalse(br27._voidFlag);
        assertFalse(br27._largeSizeFlag);
        assertEquals(020000, br27._lowerLimitNormalized);
        assertEquals(020017, br27._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 3, 0), br27._accessLock);

        BaseRegister br28 = processors._instructionProcessor.getBaseRegister(28);
        assertTrue(br28._voidFlag);

        BaseRegister br29 = processors._instructionProcessor.getBaseRegister(29);
        assertTrue(br29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600006,0 . 16 words of data",
            "BDENTRY3  + 0,0       . void",
            "",
            "$(2)      . useful bank data, will be BDI 06",
            "          $res 16",
            "",
            "$(1),START$*",
            "          LBE       B27,BDENTRY1,,B2",
            "          LBE       B29,BDENTRY3,,B2",
            "          HALT      0                . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        BaseRegister br27 = processors._instructionProcessor.getBaseRegister(27);
        assertFalse(br27._voidFlag);
        assertFalse(br27._largeSizeFlag);
        assertEquals(01000, br27._lowerLimitNormalized);
        assertEquals(01017, br27._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 3, 0), br27._accessLock);

        BaseRegister br29 = processors._instructionProcessor.getBaseRegister(29);
        assertTrue(br29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_BadBank_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0601006,0 . does not exist",
            "",
            "$(1),START$*",
            "          LBE       B27,BDENTRY1   . void bank",
            "          HALT      0              . should not get here",
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
    public void loadBaseRegisterExec_BadBank_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0601006,0 . does not exist",
            "",
            "$(1),START$*",
            "          LBE       B27,BDENTRY1,,B2  . void bank",
            "          HALT      0                 . should not get here",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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
    public void loadBaseRegisterExec_BadPP_basic(
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
    public void loadBaseRegisterExec_BadPP_extended(
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

    @Test
    public void loadBaseRegisterExecDirect_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "$(1),START$*",
            "          LBED      B27,BDENTRY1   . void bank",
            "          LBED      B28,BDENTRY2   . effectively void",
            "          LBED      B29,BDENTRY3   . not void",
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

        BaseRegister br27 = processors._instructionProcessor.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        BaseRegister br28 = processors._instructionProcessor.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        BaseRegister br29 = processors._instructionProcessor.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "$(1),START$*",
            "          LBED      B27,BDENTRY1,,B2   . void bank",
            "          LBED      B28,BDENTRY2,,B2   . effectively void",
            "          LBED      B29,BDENTRY3,,B2   . not void",
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

        BaseRegister br27 = processors._instructionProcessor.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        BaseRegister br28 = processors._instructionProcessor.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        BaseRegister br29 = processors._instructionProcessor.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY   . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
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
    public void loadBaseRegisterExecDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY   . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
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
    public void loadBaseRegisterName_basicBank_basic(
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
    public void loadBaseRegisterName_basicBank_extended(
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
    public void loadBaseRegisterName_extendedBank_basic(
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
    public void loadBaseRegisterName_extendedBank_extended(
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
    public void loadBaseRegisterName_badPP_basic(
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
    public void loadBaseRegisterName_addrException_basic(
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
    public void loadBaseRegisterName_addrException_extended(
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
    public void loadBaseRegisterName_reserved_basic(
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
    public void loadBaseRegisterName_reserved_extended(
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

    @Test
    public void loadBaseRegisterUser_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600006,0 . 16 words of data",
            "BDENTRY2  + 0600007,0 . void",
            "BDENTRY3  + 0,0       . void",
            "",
            "$(2)      . void bank data, will be BDI 07",
            "$(3)      . useful bank data, will be BDI 06",
            "          $res 16",
            "",
            "$(1),START$*",
            "          LBU       B7,BDENTRY1",
            "          LBU       B8,BDENTRY2",
            "          LBU       B9,BDENTRY2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        BaseRegister br7 = processors._instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(020000, br7._lowerLimitNormalized);
        assertEquals(020017, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 3, 0), br7._accessLock);

        BaseRegister br8 = processors._instructionProcessor.getBaseRegister(8);
        assertTrue(br8._voidFlag);

        BaseRegister br9 = processors._instructionProcessor.getBaseRegister(9);
        assertTrue(br9._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600006,0 . 16 words of data",
            "BDENTRY3  + 0,0       . void",
            "",
            "$(2)      . useful bank data, will be BDI 06",
            "          $res 16",
            "",
            "$(1),START$*",
            "          LBU       B7,BDENTRY1,,B2",
            "          LBU       B9,BDENTRY3,,B2",
            "          HALT      0                . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        BaseRegister br7 = processors._instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(01000, br7._lowerLimitNormalized);
        assertEquals(01017, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 3, 0), br7._accessLock);

        BaseRegister br9 = processors._instructionProcessor.getBaseRegister(9);
        assertTrue(br9._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_BadPP_basic(
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

    @Test
    public void loadBaseRegisterUser_BadBank_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0601006,0 . does not exist",
            "",
            "$(1),START$*",
            "          LBE       B7,BDENTRY1    . non-existing bank",
            "          HALT      077            . should not get here",
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
    public void loadBaseRegisterUser_BadBank_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0601006,0 . does not exist",
            "",
            "$(1),START$*",
            "          LBU       B5,BDENTRY1,,B2   . non-existing bank",
            "          HALT      077               . should not get here",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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
    public void loadBaseRegisterUser_InvalidBank_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 01,0",
            "",
            "$(1),START$*",
            "          LBU       B7,BDENTRY1",
            "          HALT      077 . should not get here",
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
    public void loadBaseRegisterUser_InvalidBank_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 31,0",
            "",
            "$(1),START$*",
            "          LBU       B5,BDENTRY1,,B2",
            "          HALT      077 . should not get here",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
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
    public void loadBaseRegisterUser_InvalidBR0_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600004,0",
            "",
            "$(1),START$*",
            "          LBU       B0,BDENTRY1",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR0_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600004,0",
            "",
            "$(1),START$*",
            "          LBU       B0,BDENTRY1,,B2",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR1_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600004,0",
            "",
            "$(1),START$*",
            "          LBU       B1,BDENTRY1",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR1_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  + 0600004,0",
            "",
            "$(1),START$*",
            "          LBU       B1,BDENTRY1,,B2",
            "          HALT      077 . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "$(1),START$*",
            "          LBUD      B5,BDENTRY1   . void bank",
            "          LBUD      B6,BDENTRY2   . effectively void",
            "          LBUD      B7,BDENTRY3   . not void",
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

        BaseRegister br5 = processors._instructionProcessor.getBaseRegister(5);
        assertTrue(br5._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);

        BaseRegister br6 = processors._instructionProcessor.getBaseRegister(6);
        assertTrue(br6._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);

        BaseRegister br7 = processors._instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
        assertEquals(01, br7.getLowerLimit());
        assertEquals(01777, br7.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
    }

    @Test
    public void loadBaseRegisterUserDirect_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770004,0400100 . GAP/SAP erw set, large size ring=2 domain=0100",
            "          + 001000,01777    . lower limit 0100000, upper limit 0177700",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "$(1),START$*",
            "          LBUD      B5,BDENTRY1,,B2   . void bank",
            "          LBUD      B6,BDENTRY2,,B2   . effectively void",
            "          LBUD      B7,BDENTRY3,,B2   . not void",
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

        BaseRegister br5 = processors._instructionProcessor.getBaseRegister(5);
        assertTrue(br5._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);

        BaseRegister br6 = processors._instructionProcessor.getBaseRegister(6);
        assertTrue(br6._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);

        BaseRegister br7 = processors._instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertTrue(br7._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
        assertEquals(01, br7.getLowerLimit());
        assertEquals(01777, br7.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
    }

    @Test
    public void loadBaseRegisterUserDirect_BadPP_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY   . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
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
    public void loadBaseRegisterUserDirect_BadPP_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY   . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
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

    @Test
    public void loadBaseRegisterUserDirect_InvalidBR0_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "BDENTRY1  $RES 4",
            "",
            "$(1),START$*",
            "          LBUD      B0,BDENTRY1",
            "          HALT      077 . should not get here",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_InvalidBR0_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "BDENTRY1  $RES 4",
            "",
            "$(1),START$*",
            "          LBUD      B0,BDENTRY1,,B2",
            "          HALT      077 . should not get here",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     processors._instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
    }

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

    @Test
    public void testRelativeAddress_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0) . this will be 0600005 based on B13",
            "DATA14    + 0",
            "",
            "$(1) . this will be 0600004 based on B12",
            "START$*",
            "          TRA       X12,DATA12",
            "          HALT      077 . should skip",
            "          TRA       X13,DATA13",
            "          HALT      076 . should skip",
            "          TRA       X14,DATA14",
            "          HALT      075 . should skip",
            "          TRA       X15,DATA15",
            "          HALT      074 . should skip",
            "          TRA       X5,070000",    // doesn't exist
            "          HALT      0 . this one should NOT be skipped",
            "          HALT      073",
            "",
            "DATA12    + 0",
            "",
            "$(2) . this will be 0600007 based on B15",
            "DATA15    + 0",
            "",
            "$(3) . this will be 0600006 based on B14",
            "DATA13    + 0",
        };

        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(0400000_000000L, processors._instructionProcessor.getExecOrUserXRegister(12).getW());
        assertEquals(0600000_000000L, processors._instructionProcessor.getExecOrUserXRegister(13).getW());
        assertEquals(0500000_000000L, processors._instructionProcessor.getExecOrUserXRegister(14).getW());
        assertEquals(0700000_000000L, processors._instructionProcessor.getExecOrUserXRegister(15).getW());
    }

    @Test
    public void testRelativeAddressIndirect_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0) . this will be 0600005 based on B13",
            "DATA14    NOP       *DATA15",
            "",
            "$(1) . this will be 0600004 based on B12",
            "START$*",
            "          TRA       X12,*DATA12",
            "          HALT      077 . should skip",
            "          HALT      0 . this one should NOT be skipped",
            "",
            "DATA12    NOP       *DATA14",
            "",
            "$(2) . this will be 0600007 based on B15",
            "DATA15    NOP       DATA13",
            "",
            "$(3) . this will be 0600006 based on B14",
            "DATA13    $res 32",
        };

        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);
        showDebugInfo(processors);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(0600000_000000L, processors._instructionProcessor.getExecOrUserXRegister(12).getW());
    }

    @Test
    public void testRelativeAddress_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT . this will be bank 0600005 based on B2",
            "DATA",
            "",
            "$(1) . this will be bank 0600004 based on B0",
            "START$*",
            "          . base data bank on B12 through B15",
            "          LBU       B12,(0600005000000),,B2",
            "          LBU       B13,(0600005000000),,B2",
            "          LBU       B14,(0600005000000),,B2",
            "          LBU       B15,(0600005000000),,B2",
            "",
            "          . do the tests",
            "          TRA       X12,DATA,,B12",
            "          HALT      077 . should skip",
            "          TRA       X13,DATA,,B13",
            "          HALT      076 . should skip",
            "          TRA       X14,DATA,,B14",
            "          HALT      075 . should skip",
            "          TRA       X15,DATA,,B15",
            "          HALT      074 . should skip",
            "          TRA       X5,070000,,B12",       // doesn't exist
            "          HALT      0 . this one should NOT be skipped",
            "          HALT      073",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(0400000_000000L, processors._instructionProcessor.getExecOrUserXRegister(12).getW());
        assertEquals(0500000_000000L, processors._instructionProcessor.getExecOrUserXRegister(13).getW());
        assertEquals(0600000_000000L, processors._instructionProcessor.getExecOrUserXRegister(14).getW());
        assertEquals(0700000_000000L, processors._instructionProcessor.getExecOrUserXRegister(15).getW());
    }

    @Test
    public void testRelativeAddressNonRWBanks_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0) . this will be 0600005 based on B14",
            "DATA14    + 0",
            "",
            "$(1) . this will be 0600004 based on B12",
            "START$*",
            "          TRA       X13,DATA13",
            "          J         TARGET1 . should not skip",
            "          HALT      077",
            "",
            "TARGET1",
            "          TRA       X14,DATA14",
            "          J         TARGET2 . should not skip",
            "          HALT      076 . should skip",
            "",
            "TARGET2",
            "          TRA       X15,DATA15",
            "          J         DONE",
            "          HALT      075 . should skip",
            "",
            "DONE",
            "          HALT      0",
            "",
            "$(2) . this will be 0600007 based on B15",
            "DATA15    + 0",
            "",
            "$(3) . this will be 0600006 based on B13",
            "DATA13    + 0",
        };

        //  Special construction of the absolute
        Assembler.Option[] asmOptions = {};
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", source, asmOptions);

        Linker.LCPoolSpecification[] bank4PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 1),
        };

        Linker.LCPoolSpecification[] bank5PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 0),
        };

        Linker.LCPoolSpecification[] bank6PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 3),
        };

        Linker.LCPoolSpecification[] bank7PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 2),
        };

        AccessInfo lock = new AccessInfo((short)3, 100);
        AccessPermissions iBankPerms = new AccessPermissions(true, true, false);
        AccessPermissions noReadPerms = new AccessPermissions(false, false, true);
        AccessPermissions noWritePerms = new AccessPermissions(false, true, false);
        AccessPermissions noPerms = new AccessPermissions(false, false, false);

        Linker.BankDeclaration[] bankDeclarations = {
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(4)
                .setBankName("BDI4BR12")
                .setStartingAddress(01000)
                .setInitialBaseRegister(12)
                .setGeneralAccessPermissions(iBankPerms)
                .setSpecialAccessPermissions(iBankPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank4PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(5)
                .setBankName("BDI5BR14")
                .setStartingAddress(03000)
                .setInitialBaseRegister(14)
                .setGeneralAccessPermissions(noReadPerms)
                .setSpecialAccessPermissions(noReadPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank5PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(6)
                .setBankName("BDI6BR13")
                .setStartingAddress(05000)
                .setInitialBaseRegister(13)
                .setGeneralAccessPermissions(noWritePerms)
                .setSpecialAccessPermissions(noWritePerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank6PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(7)
                .setBankName("BDI7BR15")
                .setStartingAddress(07000)
                .setInitialBaseRegister(15)
                .setGeneralAccessPermissions(noPerms)
                .setSpecialAccessPermissions(noPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank7PoolSpecs)
                .build(),
        };

        Linker.Option[] linkerOptions = {};
        Linker linker = new Linker();
        AbsoluteModule absoluteModule = linker.link("TEST", bankDeclarations, 0, linkerOptions);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);
        showDebugInfo(processors);//TODO

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0, processors._instructionProcessor.getExecOrUserXRegister(5).getW());
        assertEquals(0500000_000000L, processors._instructionProcessor.getExecOrUserXRegister(13).getW());
        assertEquals(0600000_000000L, processors._instructionProcessor.getExecOrUserXRegister(14).getW());
        assertEquals(0700000_000000L, processors._instructionProcessor.getExecOrUserXRegister(15).getW());
    }

    @Test
    public void testRelativeAddressNonRWBanks_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT . this will be bank 0600005 based on B2",
            "          + 0 . nothing data",
            "",
            "$(1) . this will be bank 0600004 based on B0",
            "START$*",
            "          TRA       X13,DATA13,,B13",
            "          J         TARGET1 . should not skip",
            "          HALT      077",
            "",
            "TARGET1",
            "          TRA       X14,DATA14,,B14",
            "          J         TARGET2 . should not skip",
            "          HALT      076 . should skip",
            "",
            "TARGET2",
            "          TRA       X15,DATA15,,B15",
            "          J         DONE",
            "          HALT      075 . should skip",
            "",
            "DONE",
            "          HALT      0",
            "",
            "$(2)",
            "DATA13    0 . BDI 0600006 will be based on B13",
            "",
            "$(4)",
            "DATA14    0 . BDI 0600007 will be based on B14",
            "",
            "$(6)",
            "DATA15    0 . BDI 0600010 will be based on B15",
        };

        //  Special construction of the absolute
        Assembler.Option[] asmOptions = {};
        Assembler asm = new Assembler();
        RelocatableModule relModule = asm.assemble("TEST", source, asmOptions);

        Linker.LCPoolSpecification[] bank4PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 1),
        };

        Linker.LCPoolSpecification[] bank5PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 0),
        };

        Linker.LCPoolSpecification[] bank6PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 2),
        };

        Linker.LCPoolSpecification[] bank7PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 4),
        };

        Linker.LCPoolSpecification[] bank8PoolSpecs = {
            new Linker.LCPoolSpecification(relModule, 6),
        };

        AccessInfo lock = new AccessInfo((short)3, 100);
        AccessPermissions iBankPerms = new AccessPermissions(true, true, false);
        AccessPermissions noReadPerms = new AccessPermissions(false, false, true);
        AccessPermissions noWritePerms = new AccessPermissions(false, true, false);
        AccessPermissions noPerms = new AccessPermissions(false, false, false);

        Linker.BankDeclaration[] bankDeclarations = {
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(4)
                .setBankName("BDI4BR0")
                .setStartingAddress(01000)
                .setInitialBaseRegister(0)
                .setGeneralAccessPermissions(iBankPerms)
                .setSpecialAccessPermissions(iBankPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank4PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(5)
                .setBankName("BDI5BR2")
                .setStartingAddress(01000)
                .setInitialBaseRegister(2)
                .setGeneralAccessPermissions(noWritePerms)
                .setSpecialAccessPermissions(noWritePerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank5PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(6)
                .setBankName("BDI6BR13")
                .setStartingAddress(01000)
                .setInitialBaseRegister(13)
                .setGeneralAccessPermissions(noReadPerms)
                .setSpecialAccessPermissions(noReadPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank6PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(7)
                .setBankName("BDI7BR14")
                .setStartingAddress(01000)
                .setInitialBaseRegister(14)
                .setGeneralAccessPermissions(noWritePerms)
                .setSpecialAccessPermissions(noWritePerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank7PoolSpecs)
                .build(),
            new Linker.BankDeclaration.Builder()
                .setBankLevel(6)
                .setBankDescriptorIndex(8)
                .setBankName("BDI8BR15")
                .setStartingAddress(01000)
                .setInitialBaseRegister(15)
                .setGeneralAccessPermissions(noPerms)
                .setSpecialAccessPermissions(noPerms)
                .setAccessInfo(lock)
                .setPoolSpecifications(bank8PoolSpecs)
                .build(),
        };

        Linker.Option[] linkerOptions = {};
        Linker linker = new Linker();
        AbsoluteModule absoluteModule = linker.link("TEST", bankDeclarations, 0, linkerOptions);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);

        startAndWait(processors._instructionProcessor);
        showDebugInfo(processors);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        assertEquals(0500000_000000L, processors._instructionProcessor.getExecOrUserXRegister(13).getW());
        assertEquals(0600000_000000L, processors._instructionProcessor.getExecOrUserXRegister(14).getW());
        assertEquals(0700000_000000L, processors._instructionProcessor.getExecOrUserXRegister(15).getW());
    }

    //  TODO testRelativeAddressRange ... some day when we care more about it

    //  TODO lots of testVirtualAddress tests
}
