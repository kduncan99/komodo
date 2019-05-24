/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_JumpInstructions extends Test_InstructionProcessor {

    @Test
    public void jump_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          NOP",
            "          J         TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jump_normal_basic_referenceViolation(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          NOP",
            "          J         050000",
            "          HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jump_normal_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          NOP",
            "          J         TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jump_indexed_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          NOP",
            "          LXM,U     X5,3",
            "          J         TARGET,X5",
            "",
            "TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "          HALT      3 . Jump here",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jump_key_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LXM,U     X5,3",
            "          LXI,U     X5,1",
            "          JK        TARGET,*X5 . Will not conditionalJump, will drop through",
            "",
            "TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01_000004L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void haltJump_74_05_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          NOP",
            "          HJ        TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, processors._instructionProcessor.getLatestStopReason());
        assertEquals(absoluteModule._entryPointAddress + 4, processors._instructionProcessor.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void haltJump_74_15_05_normal_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, processors._instructionProcessor.getLatestStopReason());
        assertEquals(absoluteModule._entryPointAddress + 4, processors._instructionProcessor.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void haltJump_74_15_05_pp1_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          HLTJ      TARGET . should throw interrupt",
            "          HALT      077    . should not get here",
            "TARGET "
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_pp1_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void storeLocationAndJump(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          SLJ       SUBROUTINE",
            "          $GFORM    6,072,4,01,4,0,4,0,1,0,1,0,16,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          + 0                    . where return address is stored",
            "          LA,U      A0,5         . update A0 value",
            "          J         *SUBROUTINE  . return to caller",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void loadModifierAndJump(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          LMJ       X11,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          LA,U      A0,5         . update A0 value",
            "          J         0,X11        . return to caller",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpZero(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          JZ        A0,GO_HERE",
            "          HALT      077          . should not happen",
            "GO_HERE",
            "          JZ        A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void doubleJumpZero(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          LA,U      A3,1",
            "          DJZ       A0,GO_HERE",
            "          HALT      077          . should not happen",
            "GO_HERE",
            "          DJZ       A2,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNonZero(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          JNZ       A1,GO_HERE",
            "          HALT      077          . should not happen",
            "GO_HERE",
            "          JNZ       A0,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpPositive(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          LNA,U     A2,1",
            "          SNZ       A3",
            "          JP        A0,GO_HERE",
            "          HALT      077          . should not happen",
            "GO_HERE",
            "          JP        A1,AND_HERE",
            "          HALT      076          . nor this",
            "AND_HERE",
            "          JP        A2,NOT_HERE",
            "          JP        A3,NOR_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      075",
            "NOR_HERE",
            "          HALT      074",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpPositiveAndShift(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,XU     A1,0777776",
            "          JPS       A0,GO_HERE",
            "          HALT      077           . should not get here",
            "GO_HERE",
            "          JPS       A1,NOT_HERE",
            "          HALT      0             . should stop here",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNegative(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          LNA,U     A2,1",
            "          SNZ       A3",
            "          JN        A2,GO_HERE",
            "          HALT      077          . should not happen",
            "GO_HERE",
            "          JN        A3,AND_HERE",
            "          HALT      076          . nor this",
            "AND_HERE",
            "          JN        A0,NOT_HERE",
            "          JN        A1,NOR_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      075",
            "NOR_HERE",
            "          HALT      074",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNegativeAndShift(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,XU     A1,0777776",
            "          JNS       A1,GO_HERE",
            "          HALT      077           . should not get here",
            "GO_HERE",
            "          JNS       A0,NOT_HERE",
            "          HALT      0             . should stop here",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpGreaterAndDecrement(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,XU     A1,0777776",
            "          LA,U      A2,0",
            "          JGD       A1,BAD1       . should not happen (a1 is 015)",
            "          JGD       A2,BAD2       . also should not happen",
            "",
            "LOOP",
            "          AA,U      A2,2          . should happen 9 times",
            "          JGD       A0,LOOP       . should happen 8 times",
            "          HALT      0             . should finish here",
            "",
            "BAD1      HALT      077",
            "BAD2      HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_777777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(021, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void jumpModifierGreaterAndIncrement_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*         .",
            "          LXI,U     X0,02           . set up X0 so we take a conditionalJump",
            "          LXM,U     X0,02           .",
            "          JMGI      X0,TARGET1      . we should take this conditionalJump",
            "          HALT      077             . should not happen",
            "",
            "TARGET1             .",
            "          LXI,U     X1,02           . set up X1 so we do not take a conditionalJump",
            "          LXM,U     X1,0            .",
            "          JMGI      X1,BAD1         . should not happen",
            "",
            "          LXI,U     X2,01           . set up X2 so we take a conditionalJump to zero,",
            "          LXM,U     X2,TARGET2      .   but indexed by the address in X2",
            "          JMGI      X2,0,*X2        . should take this",
            "          HALT      075             . should not happen",
            "",
            "TARGET2             .",
            "          LXI,XU    X3,0777776      . set up X3 so we take a conditionalJump",
            "          LXM,U     X3,010          .",
            "          LXI,U     X4,02           . set up X4 so we take a conditionalJump to zero,",
            "          LXM,U     X4,TARGET3      .    indexed by X4",
            "          JMGI      X3,0,*X4        . should take this",
            "          HALT      074             . should not happen",
            "",
            "TARGET3             .",
            "          HALT      0               . should finish here.",
            "",
            "BAD1      HALT      076             . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0_000002_000004L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0_000002_000002L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(0_000001_022014L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
        assertEquals(0_777776_000007L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
        assertEquals(0_000002_022023L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
    }

    @Test
    public void jumpCarry(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate carry",
            "          JC        BAD                 . Should not conditionalJump",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JC        DONE                . Should conditionalJump",
            "          HALT      077",
            "",
            "BAD       HALT      076",
            "",
            "DONE      HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoCarry(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate carry",
            "          JNC       TARGET              . Should conditionalJump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JNC       BAD                 . Should not conditionalJump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpOverflow(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "$(0)",
            "DATA      + 0377777777777",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate overflow",
            "          JO        BAD                 . Should not conditionalJump",
            "          LA        A1,DATA,,B2",
            "          AA        A1,DATA,,B2         . This generates overflow",
            "          JO        DONE                . Should conditionalJump",
            "          HALT      077",
            "",
            "BAD       HALT      076",
            "",
            "DONE      HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoOverflow(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 0377777777777",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate overflow",
            "          JNO       TARGET              . Should conditionalJump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA        A1,DATA,,B2",
            "          AA        A1,DATA,,B2         . This generates overflow",
            "          JNO       BAD                 . Should not conditionalJump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //TODO many tests above need both basic AND extended versions

    //TODO Need unit tests for JDF, JNDF, JFO, JNFO, JFU, JNFU

    //TODO  need tests for jumps to invalid destinations, once we can handle interrupts
}
