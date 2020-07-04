/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;


import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_FixedPointBinaryInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void addAccumulator(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LA,U      A0,7",
            "          AA,U      A0,014",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(023, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_posZeros(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          AA,U      A0,0",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_negZeros(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          SNZ       A5",
            "          LA        A0,A5",
            "          AA        A0,A5",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777777L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeAccumulator(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A0,DATA1,,B2",
            "          ANA       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777775L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addMagnitudeAccumulator_positive(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A0,DATA1,,B2",
            "          AMA       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0472, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addMagnitudeAccumulator_negative(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0234",
            "DATA2     + 0777777,0777775",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A0,DATA1,,B2",
            "          AMA       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0236, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeMagnitudeAccumulator(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LA        A0,DATA1,,B2",
            "          ANMA      A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0777777_777775L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulatorUpper(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LA,U      A0,007",
            "          AU,U      A0,014",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(07, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(023, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeAccumulatorUpper(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LA,U      A0,007",
            "          ANU,U     A0,014",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(07, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_777777_777772L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addIndexRegister(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LX,U      X0,007",
            "          AX,U      X0,014",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(023, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeIndexRegister(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LX,U      X0,007",
            "          ANX,U     X0,014",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_777777_777772L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_Overflow(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 0377777777777",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0400) . op trap enabled",
            "          LA        A0,DATA,,B2",
            "          AA        A0,DATA,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01022, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void addHalves(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0123,0555123",
            "DATA2     + 01,0223000",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A5,DATA1,,B2",
            "          AH        A5,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000124_000124L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeHalves(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0000123,0555123",
            "DATA2     + 0777776,0332123",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A3,DATA1,,B2",
            "          ANH       A3,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000124_223000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addThirds(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 000123555123",
            "DATA2     + 000001223000",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A5,DATA1,,B2",
            "          AT        A5,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000124_770124L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeThirds(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 00001,00122,05123",
            "DATA2     + 00000,02355,03000",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA        A3,DATA1,,B2",
            "          ANT       A3,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_0001_5544_2123L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void divideInteger(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 011416",
            "          + 0110621,0672145",
            "DATA2     + 01,0635035",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          DL        A2,DATA1,,B2",
            "          DI        A2,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_005213_747442L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        Assert.assertEquals(0_000000_244613L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void divideInteger_byZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111,0222222",
            "          + 0333333,0444444",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "          DL        A0,DATA1,,B2",
            "",
            "          DI,U      A0,0",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void divideInteger_byZero_noInterrupt(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111,0222222",
            "          + 0333333,0444444",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (0,0)",
            "",
            "          DL        A0,DATA1,,B2",
            "          DI,U      A0,0",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void divideInteger_byNegativeZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA1     + 0111111,0222222",
            "          + 0333333,0444444",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "          DL        A0,DATA1,,B2",
            "",
            "          DL        A0,DATA1,,B2",
            "          DI,XU     A0,0777777",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void divideSingleFractional(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 07236",
            "DATA2     + 01711467",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (0,0)",
            "",
            "          LA        A3,DATA1,,B2",
            "          DSF       A3,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        _instructionProcessor.getDesignatorRegister().setArithmeticExceptionEnabled(true);
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_001733_765274L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    @Test
    public void divideSingleFractional_byZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111222222",
            "DATA2     + 0",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "          DL        A0,DATA1,,B2",
            "",
            "          LA        A0,DATA1,,B2",
            "          DSF       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void divideSingleFractional_byZero_noInterrupt(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111222222",
            "DATA2     + 0",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (0,0)",
            "",
            "          LA        A0,DATA1,,B2",
            "          DSF       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void divideSingleFractional_byNegativeZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA1     + 0111111222222",
            "DATA2     + 0777777777777",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "",
            "          LA        A0,DATA1,,B2",
            "          DSF       A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void divideFractional(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0",
            "          + 061026335",
            "DATA2     + 01300",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (0,0)",
            "",
            "          DL        A4,DATA1,,B2",
            "          DF        A4,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000000_021653L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
        Assert.assertEquals(0_000000_000056L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void divideFractional_byZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111222222",
            "          + 0333333444444",
            "DATA2     + 0",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "",
            "          DL        A0,DATA1,,B2",
            "          DF        A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void divideFractional_byZero_noInterrupt(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0111111222222",
            "          + 0333333444444",
            "DATA2     + 0",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (0,0)",
            "",
            "          DL        A0,DATA1,,B2",
            "          DF        A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getDivideCheck());
    }

    @Test
    public void divideFractional_byNegativeZero(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA1     + 0111111222222",
            "          + 0333333444444",
            "DATA2     + 0777777777777",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1, 0)",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0100) . arith excep enabled",
            "",
            "          DL        A0,DATA1,,B2",
            "          DF        A0,DATA2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01020, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void doubleAdd(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "ADDEND1   + 0111111222222",
            "          + 0333333444444",
            "ADDEND2   + 0222222333333",
            "          + 0000000111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+ADDEND1, 0)",
            "          LD        (0,0)",
            "",
            "          DL        A0,ADDEND1,,B2",
            "          DA        A0,ADDEND2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_333333_555555L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_333333_555555L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void doubleAddNegative(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "ADDEND1   + 0333333222222",
            "          + 0777777666666",
            "ADDEND2   + 0111111222222",
            "          + 0444444333333",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+ADDEND1, 0)",
            "          LD        (0,0)",
            "",
            "          DL        A0,ADDEND1,,B2",
            "          DAN       A0,ADDEND2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_222222_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_333333_333333L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void multiplyInteger(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "",
            "          LA        A0,(0377777777777)",
            "          MI        A0,(0377777777777)",
            "          LA        A2,(0777777777776)",
            "          MI        A2,(0002244113355)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_177777_777777L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_000000_000001L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_777777_777777L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
        Assert.assertEquals(0_775533_664422L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void multiplySingleInteger(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "",
            "          LA,U      A0,200",
            "          MSI       A0,(520)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(200 * 520L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void multiplySingleInteger_overflow(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LD        (021,0)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "",
            "          LD        (0,0)",
            "",
            "          LA        A0,(0200000000000)",
            "          MSI       A0,(0300000000000)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01022, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void multiplyFractional(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "FACTOR1   0200000000002",
            "          0777777777777",
            "FACTOR2   0111111111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+FACTOR1, 0)",
            "          LD        (0,0)",
            "",
            "          LA        A3,FACTOR1,,B2",
            "          LA        A4,FACTOR1+1,,B2",
            "          MF        A3,FACTOR2,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_044444_444445L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A3).getW());
        Assert.assertEquals(0_044444_444444L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    @Test
    public void add1(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 5",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0777776,0111111",
            "DATA2     + 0,07777,0",
            "DATA3     + 0777777777776",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          ADD1,H1   DATA1,,B2",
            "          ADD1,T2   DATA2,,B2",
            "          ADD1      DATA3,,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_777777_111111L, bankData[0]);
        assertEquals(0_0000_0001_0000L, bankData[1]);
        assertEquals(0_000000_000000L, bankData[2]);

        //  check overflow and carry from the last instruction
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void add1_badPrivilege(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  In basic mode, PP of zero is required
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LD        (000001,000000)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "          GOTO      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "",
            "$(3)      $LIT",
            "BASIC",
            "          LD        (016,0) . Basic Mode, pp=3",
            "          ADD1      (0)",
            "          HALT      077 . should not get here.  if we do,",
            "                        . it's an invalid instruction (because PP>0)",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void sub1(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This unit test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 5",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 05555,0001,05555",
            "DATA2     + 0",
            "DATA3     + 0",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          SUB1,T2   DATA1,,B2      . 1's comp, carry/overflow valid",
            "          SUB1,H1   DATA2,,B2      . 2's comp, carry/overflow undefined",
            "          SUB1      DATA3,,B2      . 1's comp, carry/overflow valid",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_5555_0000_5555L, bankData[0]);
        assertEquals(0_777777_000000L, bankData[1]);
        assertEquals(0_777777_777776L, bankData[2]);

        //  check overflow and carry from the last instruction
        assertTrue(_instructionProcessor.getDesignatorRegister().getCarry());
        assertFalse(_instructionProcessor.getDesignatorRegister().getOverflow());
    }

    @Test
    public void sub1_badPrivilege(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  In basic mode, PP of zero is required
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      $LIT",
            "START",
            "          LD        (000001,000000)",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LD        (0,0)",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "          GOTO      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "",
            "$(3)      $LIT",
            "BASIC",
            "          LD        (016,0) . Basic Mode, pp=3",
            "          SUB1      (0)",
            "          HALT      077 . should not get here.  if we do,",
            "                        . it's an invalid instruction (because PP>0)",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void inc(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0",
            "DATA2     + 0777777777776",
            "DATA3     + 010111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          INC       DATA1,,B2",
            "          LA,U      A0,1                . should be skipped",
            "          INC       DATA2,,B2",
            "          LA,U      A1,1                . should be skipped",
            "          INC,H1    DATA3,,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_000000_000001L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000011_111111L, bankData[2]);
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_1L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 01",
            "DATA2     + 0777777777777",
            "DATA3     + 010111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          DEC       DATA1,,B2",
            "          LA,U      A0,1                . should be skipped",
            "          DEC       DATA2,,B2",
            "          LA,U      A1,1                . should be skipped",
            "          DEC,H1    DATA3,,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_777777_777776L, bankData[1]);
        assertEquals(0_000007_111111L, bankData[2]);
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_1L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void inc2(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0",
            "DATA2     + 0777777777775",
            "DATA3     + 010111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          INC2      DATA1,,B2",
            "          LA,U      A0,1                . should be skipped",
            "          INC2      DATA2,,B2",
            "          LA,U      A1,1                . should be skipped",
            "          INC2,H1   DATA3,,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_000000_000002L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000012_111111L, bankData[2]);
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_1L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec2(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 02",
            "DATA2     + 0",
            "DATA3     + 010,0111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          DEC2      DATA1,,B2",
            "          LA,U      A0,1                . should be skipped",
            "          DEC2      DATA2,,B2",
            "          LA,U      A1,1                . should be skipped",
            "          DEC2,H1   DATA3,,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_777777_777775L, bankData[1]);
        assertEquals(0_000006_111111L, bankData[2]);
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_1L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void enz(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //  This is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA1     + 0",
            "DATA2     + 0777777,0777777",
            "DATA3     + 010,0111111",
            "",
            "$(1)      $LIT",
            "START",
            "          LBU       B2,(LBDIREF$+DATA1,0)",
            "          LD        (0,0)",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          ENZ       DATA1,,B2",
            "          LA,U      A0,1                . should be skipped",
            "          ENZ       DATA2,,B2",
            "          LA,U      A1,1                . should be skipped",
            "          ENZ,H1    DATA3,,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] bankData = getBankByBaseRegister(2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000010_111111L, bankData[2]);
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_0L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
        Assert.assertEquals(0_1L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }
}
