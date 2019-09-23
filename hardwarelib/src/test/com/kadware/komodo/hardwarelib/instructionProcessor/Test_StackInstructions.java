/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.minalib.AbsoluteModule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_StackInstructions extends BaseFunctions {

    @Test
    public void buySimple18(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1),START$*",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACKSIZE+01000",
            "          BUY       0,*X5,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000 + (128 - 16), processors._instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, processors._instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void buySimple24(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "XVALUE    $GFORM 12,FRAMESIZE,24,STACKSIZE+01000",
            "",
            "$(1),START$*",
            "          LXSI,U    X5,FRAMESIZE",
            "          LXLM      X5,XVALUE,,B2",
            "          BUY       0,*X5,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setExecutive24BitIndexingEnabled(true);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000 + (128 - 16), processors._instructionProcessor.getExecOrUserXRegister(5).getXM24());
        assertEquals(16, processors._instructionProcessor.getExecOrUserXRegister(5).getXI12());
    }

    @Test
    public void sellSimple18(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1),START$*",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACKSIZE+01000-FRAMESIZE",
            "          SELL      0,*X5,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000 + 128, processors._instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, processors._instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void sellSimple24(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "XVALUE    $GFORM 12,FRAMESIZE,24,STACKSIZE+01000-FRAMESIZE",
            "",
            "$(1),START$*",
            "          LXSI,U    X5,FRAMESIZE",
            "          LXLM      X5,XVALUE,,B2",
            "          SELL      0,*X5,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setExecutive24BitIndexingEnabled(true);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000 + 128, processors._instructionProcessor.getExecOrUserXRegister(5).getXM24());
        assertEquals(16, processors._instructionProcessor.getExecOrUserXRegister(5).getXI12());
    }

    @Test
    public void buyWithDisplacement(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1),START$*",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACKSIZE+01000",
            "          BUY       010,*X5,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01000 + (128 - 16) - 010, processors._instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, processors._instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void buyOverflow(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1),START$*",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,01000",
            "          BUY       0,*X5,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01013, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(0, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
        assertEquals(01000, processors._instructionProcessor.getGeneralRegister(5).getH2());
        assertEquals(16, processors._instructionProcessor.getGeneralRegister(5).getH1());
    }

    @Test
    public void sellUnderflow(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1),START$*",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACKSIZE+01000",
            "          SELL      0,*X5,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);

        assertEquals(01013, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(01, processors._instructionProcessor.getLastInterrupt().getShortStatusField());
        assertEquals(01000+128, processors._instructionProcessor.getGeneralRegister(5).getH2());
        assertEquals(16, processors._instructionProcessor.getGeneralRegister(5).getH1());
    }
}
