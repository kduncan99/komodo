/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.minalib.AbsoluteModule;
import static org.junit.Assert.*;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ConditionalJumpInstructions extends BaseFunctions {

    @Test
    public void doubleJumpZero_basic(
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

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void doubleJumpZero_extended(
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpCarry_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate carry",
            "          JC        BAD                 . Should not jump",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JC        DONE                . Should jump",
            "          HALT      077",
            "",
            "BAD       HALT      076",
            "",
            "DONE      HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpCarry_extended(
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
            "          JC        BAD                 . Should not jump",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JC        DONE                . Should jump",
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpDivideFault_basic1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "ZERO      + 0",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,ZERO",
            "          JDF       DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck()); //  cleared by JDF
    }

    @Test
    public void jumpDivideFault_basic2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "DATA      + 05",
            "",
            "$(1),START$*",
            "          LA,U      A0,0100000_000000",
            "          LA,U      A1,020",
            "          DI        A0,DATA",
            "          JDF       DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck()); //  cleared by JDF
    }

    @Test
    public void jumpDivideFault_basic3(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "DATA      + 5",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,DATA",
            "          JDF       BAD",
            "          HALT      0",
            "",
            "BAD",
            "          HALT      077",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void jumpDivideFault_extended1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "ZERO      + 0",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,ZERO,,B2",
            "          JDF       DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck()); //  cleared by JDF
    }

    @Test
    public void jumpDivideFault_extended2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 05",
            "",
            "$(1),START$*",
            "          LA,U      A0,0100000_000000",
            "          LA,U      A1,020",
            "          DI        A0,DATA,,B2",
            "          JDF       DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck()); //  cleared by JDF
    }

    @Test
    public void jumpDivideFault_extended3(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 5",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,DATA,,B2",
            "          JDF       BAD",
            "          HALT      0",
            "",
            "BAD",
            "          HALT      077",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void jumpFloatingOverflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 22)",
            "          AND       A5,(0777777757777)",
            "          LD        A6",
            "          JFO       BAD1               . should not jump",
            "",
            "          OR        A5,(020000)        . set characteristic overflow",
            "          LD        A6",
            "          JFO       DONE               . should jump",
            "          HALT      076",
            "",
            "DONE",
            "          HALT      0             . should finish here",
            "",
            "BAD1      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpFloatingOverflow_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 22)",
            "          AND       A5,(0777777757777),,B2",
            "          LD        A6",
            "          JFO       BAD1               . should not jump",
            "",
            "          OR        A5,(020000),,B2    . set characteristic overflow",
            "          LD        A6",
            "          JFO       DONE               . should jump",
            "          HALT      076",
            "",
            "DONE",
            "          HALT      0             . should finish here",
            "",
            "BAD1      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpFloatingUnderflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic underflow (bit 21)",
            "          AND       A5,(0777777737777)",
            "          LD        A6",
            "          JFU       BAD1               . should not jump",
            "",
            "          OR        A5,(040000)        . set characteristic underflow",
            "          LD        A6",
            "          JFU       DONE               . should jump",
            "          HALT      076",
            "",
            "DONE",
            "          HALT      0             . should finish here",
            "",
            "BAD1      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpFloatingUnderflow_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 21)",
            "          AND       A5,(0777777737777),,B2",
            "          LD        A6",
            "          JFU       BAD1               . should not jump",
            "",
            "          OR        A5,(040000),,B2    . set characteristic underflow",
            "          LD        A6",
            "          JFU       DONE               . should jump",
            "          HALT      076",
            "",
            "DONE",
            "          HALT      0             . should finish here",
            "",
            "BAD1      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpGreaterAndDecrement_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
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

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(021, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void jumpGreaterAndDecrement_extended(
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
            "          JGD       A1,BAD1       . should not jump, A1:=0777777_777775",
            "          JGD       A2,BAD2       . also should not jump, A2:=0777777_777777",
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777776L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(021, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void jumpLowBit_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0777      . set up initial values",
            "          LA,U      A1,0776",
            "          JB        A0,GO_HERE",
            "          HALT      077          . should not happen",
            "          $RES      020000",
            "",
            "GO_HERE",
            "          JB        A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpLowBit_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0777      . set up initial values",
            "          LA,U      A1,0776",
            "          JB        A0,GO_HERE",
            "          HALT      077          . should not happen",
            "          $RES      020000",
            "",
            "GO_HERE",
            "          JB        A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
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
            "          LXI,U     X0,02           . set up X0 so we take a jump",
            "          LXM,U     X0,02           .",
            "          JMGI      X0,TARGET1      . we should take this jump",
            "          HALT      077             . should not happen",
            "",
            "TARGET1             .",
            "          LXI,U     X1,02           . set up X1 so we do not take a jump",
            "          LXM,U     X1,0            .",
            "          JMGI      X1,BAD1         . should not happen",
            "",
            "          LXI,U     X2,01           . set up X2 so we take a conditionalJump to zero,",
            "          LXM,U     X2,TARGET2      .   but indexed by the address in X2",
            "          JMGI      X2,0,*X2        . should take this",
            "          HALT      075             . should not happen",
            "",
            "TARGET2             .",
            "          LXI,XU    X3,0777776      . set up X3 so we take a jump",
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000002_000004L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
        Assert.assertEquals(0_000002_000002L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
        Assert.assertEquals(0_000001_022014L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
        Assert.assertEquals(0_777776_000007L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
        Assert.assertEquals(0_000002_022023L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
    }

    @Test
    public void jumpModifierGreaterAndIncrement_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*         .",
            "          LXI,U     X0,02           . set up X0 so we take a jump",
            "          LXM,U     X0,02           .",
            "          JMGI      X0,TARGET1      . we should take this jump",
            "          HALT      077             . should not happen",
            "",
            "TARGET1             .",
            "          LXI,U     X1,02           . set up X1 so we do not take a jump",
            "          LXM,U     X1,0            .",
            "          JMGI      X1,BAD1         . should not happen",
            "",
            "          LXI,U     X2,01           .",
            "          LXM,U     X2,2            .",
            "          JMGI      X2,TARGET2      . should take this and increment X2",
            "          HALT      075             . should not happen",
            "",
            "TARGET2             .",
            "          LXI,XU    X3,0777776      . set up X3 so we take a jump",
            "          LXM,U     X3,010          .",
            "          LXI,U     X4,02           . set up X4 so we take a jump to zero,",
            "          LXM,U     X4,TARGET3      .    indexed by X4",
            "          JMGI      X3,0,*X4        . should take this",
            "          HALT      074             . should not happen",
            "",
            "TARGET3             .",
            "          HALT      0               . should finish here.",
            "",
            "BAD1      HALT      076             . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000002_000004L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
        Assert.assertEquals(0_000002_000002L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
        Assert.assertEquals(0_000001_000003L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
        Assert.assertEquals(0_777776_000007L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
        Assert.assertEquals(0_000002_001023L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
    }

    @Test
    public void jumpNegative_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          LNA,U     A2,1",
            "          SNZ       A3",
            "          JN        A2,GO_HERE",
            "          HALT      077          . should not happen",
            "",
            "GO_HERE",
            "          JN        A3,AND_HERE",
            "          HALT      076          . nor this",
            "          $RES      030000",
            "",
            "AND_HERE",
            "          JN        A0,NOT_HERE",
            "          JN        A1,NOR_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      075",
            "",
            "NOR_HERE",
            "          HALT      074",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNegative_extended(
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
            "",
            "GO_HERE",
            "          JN        A3,AND_HERE",
            "          HALT      076          . nor this",
            "          $RES      030000",
            "",
            "AND_HERE",
            "          JN        A0,NOT_HERE",
            "          JN        A1,NOR_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      075",
            "",
            "NOR_HERE",
            "          HALT      074",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNegativeAndShift_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
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

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNegativeAndShift_extended(
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNoCarry_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate carry",
            "          JNC       TARGET              . Should jump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JNC       BAD                 . Should not jump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoCarry_extended(
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
            "          JNC       TARGET              . Should jump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA,U      A1,2",
            "          AA,XU     A1,0777776          . This generates a carry",
            "          JNC       BAD                 . Should not jump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoDivideFault_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "DATA      + 5",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,DATA",
            "          JNDF      DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void jumpNoDivideFault_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 5",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          LA,U      A1,020",
            "          DI        A0,DATA,,B2",
            "          JNDF      DONE",
            "          HALT      077",
            "",
            "DONE",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertFalse(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void jumpNoFloatingOverflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 22)",
            "          AND       A5,(0777777757777)",
            "          LD        A6",
            "          JNFO      OK                 . should jump",
            "          HALT      077",
            "",
            "OK",
            "          OR        A5,(020000)        . set characteristic overflow",
            "          LD        A6",
            "          JNFO      BAD2               . should not jump",
            "          HALT      0",
            "",
            "BAD2      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoFloatingOverflow_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 22)",
            "          AND       A5,(0777777757777),,B2",
            "          LD        A6",
            "          JNFO      OK                 . should jump",
            "          HALT      077",
            "",
            "OK",
            "          OR        A5,(020000),,B2    . set characteristic overflow",
            "          LD        A6",
            "          JNFO      BAD2               . should not jump",
            "          HALT      0",
            "",
            "BAD2      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoFloatingUnderflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic underflow (bit 21)",
            "          AND       A5,(0777777737777)",
            "          LD        A6",
            "          JNFU      OK                 . should jump",
            "          HALT      077",
            "",
            "OK",
            "          OR        A5,(040000)        . set characteristic underflow",
            "          LD        A6",
            "          JNFU      BAD2               . should not jump",
            "          HALT      0",
            "",
            "BAD2      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoFloatingUnderflow_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //TODO
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          SD        A5                 . clear characteristic overflow (bit 21)",
            "          AND       A5,(0777777737777),,B2",
            "          LD        A6",
            "          JNFU      OK                 . should jump",
            "          HALT      077",
            "",
            "OK",
            "          OR        A5,(040000),,B2    . set characteristic underflow",
            "          LD        A6",
            "          JNFU      BAD2               . should not jump",
            "          HALT      0",
            "",
            "BAD2      HALT      077",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoLowBit_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0554      . set up initial values",
            "          LA,U      A1,0555",
            "          JNB       A0,GO_HERE",
            "          HALT      077          . should not happen",
            "          $RES      020000",
            "",
            "GO_HERE",
            "          JNB       A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoLowBit_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START$*",
            "          LA,U      A0,0554      . set up initial values",
            "          LA,U      A1,0555",
            "          JNB       A0,GO_HERE",
            "          HALT      077          . should not happen",
            "          $RES      020000",
            "",
            "GO_HERE",
            "          JNB       A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNonZero_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          JNZ       A1,GO_HERE",
            "          HALT      077          . should not happen",
            "",
            "GO_HERE",
            "          JNZ       A0,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNonZero_extended(
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoOverflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "DATA      + 0377777777777",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate overflow",
            "          JNO       TARGET              . Should jump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA        A1,DATA,,B2",
            "          AA        A1,DATA,,B2         . This generates overflow",
            "          JNO       BAD                 . Should not jump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpNoOverflow_extended(
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
            "          JNO       TARGET              . Should jump",
            "          HALT      077",
            "",
            "TARGET",
            "          LA        A1,DATA,,B2",
            "          AA        A1,DATA,,B2         . This generates overflow",
            "          JNO       BAD                 . Should not jump",
            "DONE      HALT      0",
            "",
            "BAD       HALT      076",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpOverflow_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0)",
            "DATA      + 0377777777777",
            "",
            "$(1),START$*",
            "          LA,U      A0,0",
            "          AA,XU     A0,1                . Does not generate overflow",
            "          JO        BAD                 . Should not Jump",
            "          LA        A1,DATA",
            "          AA        A1,DATA             . This generates overflow",
            "          JO        DONE                . Should Jump",
            "          HALT      077",
            "",
            "BAD       HALT      076",
            "",
            "DONE      HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpOverflow_extended(
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
            "          JO        BAD                 . Should not jump",
            "          LA        A1,DATA,,B2",
            "          AA        A1,DATA,,B2         . This generates overflow",
            "          JO        DONE                . Should jump",
            "          HALT      077",
            "          $RES      030000",
            "",
            "BAD       HALT      076",
            "",
            "DONE      HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpPositive_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
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

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpPositive_extended(
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpPositiveAndShift_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
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

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpPositiveAndShift_extended(
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(020L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpZero_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,1",
            "          JZ        A0,GO_HERE",
            "          HALT      077          . should not happen",
            "          $RES      020000",
            "",
            "GO_HERE",
            "          JZ        A1,NOT_HERE",
            "          HALT      0            . should stop here",
            "",
            "NOT_HERE",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void jumpZero_extended(
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
            "          $RES      020000",
            "",
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

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }
}
