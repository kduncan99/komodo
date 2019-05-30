/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPIConflictException;
import com.kadware.em2200.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_SpecialInstructions extends BaseFunctions {

    @Test
    public void nop_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       040000,*X2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(4, processors._instructionProcessor.getGeneralRegister(2).getH1());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(2).getH2());
    }

    @Test
    public void nop_basic_indirect_violation(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       *040000,*X2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(4, processors._instructionProcessor.getGeneralRegister(2).getH1());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(2).getH2());
    }

    @Test
    public void nop_basic_indirect_noViolation(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0)",
            "DATA      + 0",
            "DATA      + 0",
            "DATA      + 055000",
            "",
            "$(1),START$*",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       *DATA,*X2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(4, processors._instructionProcessor.getGeneralRegister(2).getH1());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(2).getH2());
    }

    @Test
    public void nop_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "$(0)",
            "DATA      + 0",
            "",
            "$(1),START$*",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       040000,*X2,B2  . U is out of limits, be we shouldn't care",
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
        assertEquals(4, processors._instructionProcessor.getGeneralRegister(2).getH1());
        assertEquals(6, processors._instructionProcessor.getGeneralRegister(2).getH2());
    }
}
