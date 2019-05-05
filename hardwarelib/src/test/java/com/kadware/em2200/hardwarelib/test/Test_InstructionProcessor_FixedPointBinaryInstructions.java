/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_FixedPointBinaryInstructions extends Test_InstructionProcessor {

    @Test
    public void addAccumulator(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA,U      A0,7",
            "          AA,U      A0,014",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(023, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_posZeros(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          AA,U      A0,0",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_negZeros(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          SNZ       A5",
            "          LA        A0,A5",
            "          AA        A0,A5",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777777L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertTrue(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeAccumulator(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A0,DATA1,,B2",
            "          ANA       A0,DATA2,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777775L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addMagnitudeAccumulator_positive(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A0,DATA1,,B2",
            "          AMA       A0,DATA2,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0472, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addMagnitudeAccumulator_negative(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA1     + 0234",
            "DATA2     + 0777777,0777775",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A0,DATA1,,B2",
            "          AMA       A0,DATA2,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0236, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeMagnitudeAccumulator(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA1     + 0234",
            "DATA2     + 0236",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A0,DATA1,,B2",
            "          ANMA      A0,DATA2,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0777777_777775L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulatorUpper(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA,U      A0,007",
            "          AU,U      A0,014",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(07, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(023, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeAccumulatorUpper(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA,U      A0,007",
            "          ANU,U     A0,014",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(07, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777772L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addIndexRegister(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LX,U      X0,007",
            "          AX,U      X0,014",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(023, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeIndexRegister(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LX,U      X0,007",
            "          ANX,U     X0,014",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777772L, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_Overflow(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA      + 0377777777777",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A0,DATA,,B2",
            "          AA        A0,DATA,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setOperationTrapEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01022, ip.getLatestStopDetail());
    }

    @Test
    public void addHalves(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA        A5,(000123,0555123),,B2",
            "          AH        A5,(01,0223000),,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000124_000124l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeHalves(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 0000123,0555123",
            "DATA2     + 0777776,0332123",
            "",
            "$(1),START*",
            "          LA        A3,DATA1,,B2",
            "          ANH       A3,DATA2,,B2",
            "          HALT      0",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000124_223000L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addThirds(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 000123555123",
            "DATA2     + 000001223000",
            "",
            "$(1),START*",
            "          LA        A5,DATA1,,B2",
            "          AT        A5,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000124_770124L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeThirds(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 00001,00122,05123",
            "DATA2     + 00000,02355,03000",
            "",
            "$(1),START*",
            "          LA        A3,DATA1,,B2",
            "          ANT       A3,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_0001_5544_2123L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void divideInteger(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 011416",
            "          + 0110621,0672145",
            "DATA2     + 01,0635035",
            "$(1),START*",
            "          DL        A2,DATA1,,B2",
            "          DI        A2,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_005213_747442L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_244613L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    //TODO
//    @Test
//    public void divideInteger_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void divideInteger_byZero_noInterrupt(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 0111111,0222222",
            "          + 0333333,0444444",
            "$(1),START*",
            "          DL        A0,DATA1,,B2",
            "          DI,U      A0,0",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    //TODO
//    @Test
//    public void divideInteger_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void divideSingleFractional(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 07236",
            "          + 0743464241454",
            "DATA2     + 01711467",
            "$(1),START*",
            "          DL        A3,DATA1,,B2",
            "          DSF       A3,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_001733_765274L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    //TODO
//    @Test
//    public void divideSingleFractional_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void divideSingleFractional_byZero_noInterrupt(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 0111111222222",
            "          + 0333333444444",
            "DATA2     + 0",
            "$(1),START*",
            "          DL        A0,DATA1,,B2",
            "          DSF       A0,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    //TODO
//    @Test
//    public void divideSingleFractional_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void divideFractional(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // Example from the hardware guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 0",
            "          + 061026335",
            "DATA2     + 01300",
            "$(1),START*",
            "          DL        A4,DATA1,,B2",
            "          DF        A4,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000000_021653L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_000056L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    //TODO
//    @Test
//    public void divideFractional_byZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void divideFractional_byZero_noInterrupt(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "DATA1     + 0111111222222",
            "          + 0333333444444",
            "DATA2     + 0",
            "$(1),START*",
            "          DL        A0,DATA1,,B2",
            "          DF        A0,DATA2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    //TODO
//    @Test
//    public void divideFractional_byNegativeZero(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//             //???? implement when we can catch arithmetic exception interrupt
//    }

    @Test
    public void doubleAdd(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0)",
            "          $LIT",
            "ADDEND1   + 0111111222222",
            "          + 0333333444444",
            "ADDEND2   + 0222222333333",
            "          + 0000000111111",
            "$(1),START*",
            "          DL        A0,ADDEND1",
            "          DA        A0,ADDEND2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_333333_555555L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_555555L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void doubleAddNegative(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "$(0)",
            "          $LIT",
            "ADDEND1   + 0333333222222",
            "          + 0777777666666",
            "ADDEND2   + 0111111222222",
            "          + 0444444333333",
            "$(1),START*",
            "          DL        A0,ADDEND1",
            "          DAN       A0,ADDEND2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_222222_000000L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_333333L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void multiplyInteger(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          LA        A0,(0377777777777),,B2",
            "          MI        A0,(0377777777777),,B2",
            "          LA        A2,(0777777777776),,B2",
            "          MI        A2,(0002244113355),,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_177777_777777L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000000_000001L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_777777_777777L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_775533_664422L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void multiplySingleInteger(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(0)      $LIT",
            "$(1)      .",
            "          LA,U      A0,200",
            "          MSI       A0,(520),,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(200 * 520L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    //TODO Need an MSI overflow test once we can handle interrupts

    @Test
    public void multiplyFractional(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "FACTOR1   0200000000002",
            "          0777777777777",
            "FACTOR2   0111111111111",
            "",
            "$(1),START*",
            "          LA        A3,FACTOR1,,B2",
            "          LA        A4,FACTOR1+1,,B2",
            "          MF        A3,FACTOR2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_044444_444445L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_044444_444444L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    @Test
    public void add1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          ADD1,H1   (0777776,0111111),,B2",
            "          ADD1,T2   (0,07777,0),,B2",
            "          ADD1      (0777777777776),,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_777777_111111L, bankData[0]);
        assertEquals(0_0000_0001_0000L, bankData[1]);
        assertEquals(0_000000_000000L, bankData[2]);

        //  check overflow and carry from the last instruction
        assertTrue(dReg.getCarry());
        assertFalse(dReg.getOverflow());
    }

    @Test
    public void add1_badPrivilege(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  In basic mode, PP of zero is required
        String[] source = {
            "          $BASIC",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          ADD1      (0)",
            "          HALT      0 . should not get here.  if we do,",
            "                      . it's an invalid instruction (because PP>0)",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(true);
        dReg.setProcessorPrivilege(1);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    @Test
    public void sub1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          SUB1,T2   (05555,0001,05555),,B2",
            "          SUB1,H1   (0),,B2",
            "          SUB1      (0),,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_5555_0000_5555L, bankData[0]);
        assertEquals(0_777777_000000L, bankData[1]);
        assertEquals(0_777777_777776L, bankData[2]);

        //  check overflow and carry from the last instruction
        assertFalse(dReg.getCarry());
        assertFalse(dReg.getOverflow());
    }

    @Test
    public void sub1_badPrivilege(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  In basic mode, PP of zero is required
        String[] source = {
            "          $BASIC",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          SUB1      (0)",
            "          HALT      0 . should not get here.  if we do,",
            "                      . it's an invalid instruction (because PP>0)",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(true);
        dReg.setProcessorPrivilege(1);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    @Test
    public void inc(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "          $LIT",
            "$(1),START*",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          INC       (0),,B2",
            "          LA,U      A0,1                . should be skipped",
            "          INC       (0777777777776),,B2",
            "          LA,U      A1,1                . should be skipped",
            "          INC,H1    (010111111),,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_000000_000001L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000011_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "",
            "$(0)",
            "          $LIT",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          DEC       (01),,B2",
            "          LA,U      A0,1                . should be skipped",
            "          DEC       (0777777777777),,B2",
            "          LA,U      A1,1                . should be skipped",
            "          DEC,H1    (010111111),,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_777777_777776L, bankData[1]);
        assertEquals(0_000007_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void inc2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "",
            "$(0)      $LIT",
            "$(1),START*",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          INC2      (0),,B2",
            "          LA,U      A0,1                . should be skipped",
            "          INC2      (0777777777775),,B2",
            "          LA,U      A1,1                . should be skipped",
            "          INC2,H1   (010111111),,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_000000_000002L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000012_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "",
            "$(0)      $LIT",
            "$(1),START*",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          DEC2      (02),,B2",
            "          LA,U      A0,1                . should be skipped",
            "          DEC2      (0),,B2",
            "          LA,U      A1,1                . should be skipped",
            "          DEC2,H1   (010,0111111),,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_777777_777775L, bankData[1]);
        assertEquals(0_000006_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void ienz(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
            "          $EXTEND",
            "",
            "$(0)      $LIT",
            "$(1),START*",
            "          LA,U      A0,0",
            "          LA,U      A1,0",
            "          LA,U      A2,0",
            "          ENZ       (0),,B2",
            "          LA,U      A0,1                . should be skipped",
            "          ENZ       (0777777,0777777),,B2",
            "          LA,U      A1,1                . should be skipped",
            "          ENZ,H1    (010,0111111),,B2",
            "          LA,U      A2,1                . should be executed",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000010_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }
}
