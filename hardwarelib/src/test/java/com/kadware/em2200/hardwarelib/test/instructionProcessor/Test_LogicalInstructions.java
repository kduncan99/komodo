/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import static org.junit.Assert.*;

import com.kadware.em2200.minalib.AbsoluteModule;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_LogicalInstructions extends BaseFunctions {

    @Test
    public void logicalANDBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A4,(0777777777123)",
            "          AND,U     A4,0543321",
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
        assertEquals(0_777777_777123L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_543121L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void logicalANDExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A4,(0777777777123),,B2",
            "          AND,U     A4,0543321",
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
        assertEquals(0_777777_777123L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_543121L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void logicalMLUBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A8,(0777777000000)",
            "          LR        R2,(0707070707070)",
            "          MLU       A8,(0000000777777)",
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
        assertEquals(0_777777_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_070707_707070L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void logicalMLUExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A8,(0777777000000),,B2",
            "          LR        R2,(0707070707070),,B2",
            "          MLU       A8,(0000000777777),,B2",
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
        assertEquals(0_777777_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_070707_707070L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void logicalORBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A0,(0111111111111)",
            "          OR        A0,(0222222222222)",
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
        assertEquals(0_111111_111111L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_333333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void logicalORExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A0,(0111111111111),,B2",
            "          OR        A0,(0222222222222),,B2",
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
        assertEquals(0_111111_111111L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_333333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void logicalXORBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A2,(0777000777000)",
            "          XOR,H1    A2,(0750750777777)",
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
        assertEquals(0_777000_777000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_777000_027750L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void logicalXORExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "$(1),START$*",
            "          LA        A2,(0777000777000),,B2",
            "          XOR,H1    A2,(0750750777777),,B2",
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
        assertEquals(0_777000_777000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_777000_027750L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }
}
