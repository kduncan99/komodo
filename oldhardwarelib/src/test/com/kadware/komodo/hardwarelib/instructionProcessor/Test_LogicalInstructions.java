/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_LogicalInstructions extends BaseFunctions {

    @Test
    public void logicalANDBasic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          GOTO      BMSTARTBDI",
            "BMSTARTBDI + LBDIREF$+BMSTART,BMSTART",
            "",
            "$(3),BMSTART",
            "          LD        DESREG",
            "          LA        A4,PARAMETER",
            "          AND,U     A4,0543321",
            "          HALT      0",
            "",
            "DESREG    + 02,0",
            "PARAMETER + 0777777777123",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777123L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        Assert.assertEquals(0_000000_543121L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());

        clear();
    }

    @Test
    public void logicalANDExtended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA        A4,PARAMETER",
            "          AND,U     A4,0543321",
            "          HALT      0",
            "",
            "DESREG    + 0,0",
            "PARAMETER + 0777777777123",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777123L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        Assert.assertEquals(0_000000_543121L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());

        clear();
    }

//    @Test
//    public void logicalMLUBasic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A8,(0777777000000)",
//            "          LR        R2,(0707070707070)",
//            "          MLU       A8,(0000000777777)",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777777_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
//        Assert.assertEquals(0_070707_707070L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
//    }
//
//    @Test
//    public void logicalMLUExtended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A8,(0777777000000),,B2",
//            "          LR        R2,(0707070707070),,B2",
//            "          MLU       A8,(0000000777777),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777777_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A8).getW());
//        Assert.assertEquals(0_070707_707070L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A9).getW());
//    }
//
//    @Test
//    public void logicalORBasic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A0,(0111111111111)",
//            "          OR        A0,(0222222222222)",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_111111_111111L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_333333_333333L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
//    @Test
//    public void logicalORExtended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A0,(0111111111111),,B2",
//            "          OR        A0,(0222222222222),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_111111_111111L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_333333_333333L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
//    @Test
//    public void logicalXORBasic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A2,(0777000777000)",
//            "          XOR,H1    A2,(0750750777777)",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777000_777000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0_777000_027750L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//    }
//
//    @Test
//    public void logicalXORExtended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "$(1),START$*",
//            "          LA        A2,(0777000777000),,B2",
//            "          XOR,H1    A2,(0750750777777),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777000_777000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0_777000_027750L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//    }
}
