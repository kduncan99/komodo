/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;


import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.InventoryManager;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_FixedPointBinaryInstructions extends BaseFunctions {

//    @Test
//    public void addAccumulator(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,7",
//            "          AA,U      A0,014",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(023, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addAccumulator_posZeros(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          AA,U      A0,0",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addAccumulator_negZeros(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          SNZ       A5",
//            "          LA        A0,A5",
//            "          AA        A0,A5",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeAccumulator(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0234",
//            "DATA2     + 0236",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          ANA       A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addMagnitudeAccumulator_positive(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0234",
//            "DATA2     + 0236",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          AMA       A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0472, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addMagnitudeAccumulator_negative(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0234",
//            "DATA2     + 0777777,0777775",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          AMA       A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0236, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeMagnitudeAccumulator(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0234",
//            "DATA2     + 0236",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          ANMA      A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0777777_777775L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addAccumulatorUpper(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,007",
//            "          AU,U      A0,014",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(07, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(023, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeAccumulatorUpper(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,007",
//            "          ANU,U     A0,014",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(07, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_777777_777772L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addIndexRegister(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LX,U      X0,007",
//            "          AX,U      X0,014",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(023, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeIndexRegister(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LX,U      X0,007",
//            "          ANX,U     X0,014",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_777777_777772L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addAccumulator_Overflow(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA      + 0377777777777",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA,,B2",
//            "          AA        A0,DATA,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setOperationTrapEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01022, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void addHalves(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$*",
//            "          LA        A5,(000123,0555123),,B2",
//            "          AH        A5,(01,0223000),,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_000124_000124L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeHalves(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0000123,0555123",
//            "DATA2     + 0777776,0332123",
//            "",
//            "$(1),START$*",
//            "          LA        A3,DATA1,,B2",
//            "          ANH       A3,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_000124_223000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addThirds(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 000123555123",
//            "DATA2     + 000001223000",
//            "",
//            "$(1),START$*",
//            "          LA        A5,DATA1,,B2",
//            "          AT        A5,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_000124_770124L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void addNegativeThirds(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 00001,00122,05123",
//            "DATA2     + 00000,02355,03000",
//            "",
//            "$(1),START$*",
//            "          LA        A3,DATA1,,B2",
//            "          ANT       A3,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_0001_5544_2123L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void divideInteger(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             // Example from the hardware guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 011416",
//            "          + 0110621,0672145",
//            "DATA2     + 01,0635035",
//            "",
//            "$(1),START$*",
//            "          DL        A2,DATA1,,B2",
//            "          DI        A2,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_005213_747442L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0_000000_244613L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//    }
//
//    @Test
//    public void divideInteger_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111,0222222",
//            "          + 0333333,0444444",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DI,U      A0,0",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void divideInteger_byZero_noInterrupt(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111,0222222",
//            "          + 0333333,0444444",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DI,U      A0,0",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
//    }
//
//    @Test
//    public void divideInteger_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111,0222222",
//            "          + 0333333,0444444",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DI,XU     A0,0777777",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void divideSingleFractional(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // Example from the hardware guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 07236",
//            "DATA2     + 01711467",
//            "",
//            "$(1),START$*",
//            "          LA        A3,DATA1,,B2",
//            "          DSF       A3,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_001733_765274L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//    }
//
//    @Test
//    public void divideSingleFractional_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "DATA2     + 0",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          DSF       A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void divideSingleFractional_byZero_noInterrupt(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "DATA2     + 0",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          DSF       A0,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
//    }
//
//    @Test
//    public void divideSingleFractional_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "DATA2     + 0777777777777",
//            "",
//            "$(1),START$*",
//            "          LA        A0,DATA1,,B2",
//            "          DSF       A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void divideFractional(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // Example from the hardware guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0",
//            "          + 061026335",
//            "DATA2     + 01300",
//            "",
//            "$(1),START$*",
//            "          DL        A4,DATA1,,B2",
//            "          DF        A4,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_000000_021653L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//        Assert.assertEquals(0_000000_000056L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
//    }
//
//    @Test
//    public void divideFractional_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "          + 0333333444444",
//            "DATA2     + 0",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DF        A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void divideFractional_byZero_noInterrupt(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "          + 0333333444444",
//            "DATA2     + 0",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DF        A0,DATA2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getDivideCheck());
//    }
//
//    @Test
//    public void divideFractional_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "DATA1     + 0111111222222",
//            "          + 0333333444444",
//            "DATA2     + 0777777777777",
//            "",
//            "$(1),START$*",
//            "          DL        A0,DATA1,,B2",
//            "          DF        A0,DATA2,,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01020, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void doubleAdd(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)",
//            "ADDEND1   + 0111111222222",
//            "          + 0333333444444",
//            "ADDEND2   + 0222222333333",
//            "          + 0000000111111",
//            "",
//            "$(1),START$*",
//            "          DL        A0,ADDEND1",
//            "          DA        A0,ADDEND2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_333333_555555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_333333_555555L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void doubleAddNegative(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)",
//            "ADDEND1   + 0333333222222",
//            "          + 0777777666666",
//            "ADDEND2   + 0111111222222",
//            "          + 0444444333333",
//            "",
//            "$(1),START$*",
//            "          DL        A0,ADDEND1",
//            "          DAN       A0,ADDEND2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_222222_000000L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_333333_333333L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void multiplyInteger(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          LA        A0,(0377777777777),,B2",
//            "          MI        A0,(0377777777777),,B2",
//            "          LA        A2,(0777777777776),,B2",
//            "          MI        A2,(0002244113355),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_177777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_000000_000001L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_777777_777777L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//        Assert.assertEquals(0_775533_664422L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//    }
//
//    @Test
//    public void multiplySingleInteger(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$* .",
//            "          LA,U      A0,200",
//            "          MSI       A0,(520),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(200 * 520L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//    }
//
//    @Test
//    public void multiplySingleInteger_overflow(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$* .",
//            "          LA        A0,(0200000000000),,B2",
//            "          MSI       A0,(0300000000000),,B2",
//            "          HALT      0",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01022, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void multiplyFractional(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "FACTOR1   0200000000002",
//            "          0777777777777",
//            "FACTOR2   0111111111111",
//            "",
//            "$(1),START$*",
//            "          LA        A3,FACTOR1,,B2",
//            "          LA        A4,FACTOR1+1,,B2",
//            "          MF        A3,FACTOR2,,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        Assert.assertEquals(0_044444_444445L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
//        Assert.assertEquals(0_044444_444444L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
//    }
//
//    @Test
//    public void add1(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 5",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          ADD1,H1   (0777776,0111111),,B2",
//            "          ADD1,T2   (0,07777,0),,B2",
//            "          ADD1      (0777777777776),,B2",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_777777_111111L, bankData[0]);
//        assertEquals(0_0000_0001_0000L, bankData[1]);
//        assertEquals(0_000000_000000L, bankData[2]);
//
//        //  check overflow and carry from the last instruction
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void add1_badPrivilege(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  In basic mode, PP of zero is required
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          ADD1      (0)",
//            "          HALT      0 . should not get here.  if we do,",
//            "                      . it's an invalid instruction (because PP>0)",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void sub1(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This unit test is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 5",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          SUB1,T2   (05555,0001,05555),,B2 . 1's comp, carry/overflow valid",
//            "          SUB1,H1   (0),,B2                . 2's comp, carry/overflow undefined",
//            "          SUB1      (0),,B2                . 1's comp, carry/overflow valid",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_5555_0000_5555L, bankData[0]);
//        assertEquals(0_777777_000000L, bankData[1]);
//        assertEquals(0_777777_777776L, bankData[2]);
//
//        //  check overflow and carry from the last instruction
//        assertTrue(processors._instructionProcessor.getDesignatorRegister().getCarry());
//        assertFalse(processors._instructionProcessor.getDesignatorRegister().getOverflow());
//    }
//
//    @Test
//    public void sub1_badPrivilege(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  In basic mode, PP of zero is required
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          SUB1      (0)",
//            "          HALT      0 . should not get here.  if we do,",
//            "                      . it's an invalid instruction (because PP>0)",
//            };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    @Test
//    public void inc(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          LA,U      A1,0",
//            "          LA,U      A2,0",
//            "          INC       (0),,B2",
//            "          LA,U      A0,1                . should be skipped",
//            "          INC       (0777777777776),,B2",
//            "          LA,U      A1,1                . should be skipped",
//            "          INC,H1    (010111111),,B2",
//            "          LA,U      A2,1                . should be executed",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_000000_000001L, bankData[0]);
//        assertEquals(0_000000_000000L, bankData[1]);
//        assertEquals(0_000011_111111L, bankData[2]);
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_1L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//    }
//
//    @Test
//    public void dec(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)",
//            "          $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          LA,U      A1,0",
//            "          LA,U      A2,0",
//            "          DEC       (01),,B2",
//            "          LA,U      A0,1                . should be skipped",
//            "          DEC       (0777777777777),,B2",
//            "          LA,U      A1,1                . should be skipped",
//            "          DEC,H1    (010111111),,B2",
//            "          LA,U      A2,1                . should be executed",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_000000_000000L, bankData[0]);
//        assertEquals(0_777777_777776L, bankData[1]);
//        assertEquals(0_000007_111111L, bankData[2]);
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_1L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//    }
//
//    @Test
//    public void inc2(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          LA,U      A1,0",
//            "          LA,U      A2,0",
//            "          INC2      (0),,B2",
//            "          LA,U      A0,1                . should be skipped",
//            "          INC2      (0777777777775),,B2",
//            "          LA,U      A1,1                . should be skipped",
//            "          INC2,H1   (010111111),,B2",
//            "          LA,U      A2,1                . should be executed",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_000000_000002L, bankData[0]);
//        assertEquals(0_000000_000000L, bankData[1]);
//        assertEquals(0_000012_111111L, bankData[2]);
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_1L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//    }
//
//    @Test
//    public void dec2(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          LA,U      A1,0",
//            "          LA,U      A2,0",
//            "          DEC2      (02),,B2",
//            "          LA,U      A0,1                . should be skipped",
//            "          DEC2      (0),,B2",
//            "          LA,U      A1,1                . should be skipped",
//            "          DEC2,H1   (010,0111111),,B2",
//            "          LA,U      A2,1                . should be executed",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_000000_000000L, bankData[0]);
//        assertEquals(0_777777_777775L, bankData[1]);
//        assertEquals(0_000006_111111L, bankData[2]);
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_1L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//    }
//
//    @Test
//    public void ienz(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  This is per the hardware instruction guide
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 1 3",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          LA,U      A1,0",
//            "          LA,U      A2,0",
//            "          ENZ       (0),,B2",
//            "          LA,U      A0,1                . should be skipped",
//            "          ENZ       (0777777,0777777),,B2",
//            "          LA,U      A1,1                . should be skipped",
//            "          ENZ,H1    (010,0111111),,B2",
//            "          LA,U      A2,1                . should be executed",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(false);
//        processors._instructionProcessor.getDesignatorRegister().setDivideCheck(false);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        long[] bankData = getBank(processors._instructionProcessor, 2);
//        assertEquals(0_000000_000000L, bankData[0]);
//        assertEquals(0_000000_000000L, bankData[1]);
//        assertEquals(0_000010_111111L, bankData[2]);
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        Assert.assertEquals(0_0L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//        Assert.assertEquals(0_1L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
//    }
}
