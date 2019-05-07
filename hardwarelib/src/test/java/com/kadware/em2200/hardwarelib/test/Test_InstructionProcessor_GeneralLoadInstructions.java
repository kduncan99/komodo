/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_GeneralLoadInstructions extends Test_InstructionProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Load Instructions
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void generalLoadImmediate_Extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Write some load instructions starting at absolute address 01000 for the MSP's UPI
        String[] source = {
            "          $EXTEND .",
            "$(1) . ",
            "          LA,U      A0,01000     . ",
            "          LNA,U     A1,1         . ",
            "          LNA,XU    A2,0777776   . ",
            "          LMA,U     A3,2         . ",
            "          LMA,XU    A4,0777774   . ",
            "          LNMA,U    A5,4         . ",
            "          LNMA,XU   A6,0777772   . ",
            "          LA,XU     A7,0777777   . make sure neg zero is eliminated",
            "          LX,XU     X5,01234     . ",
            "          LXI,U     X0,0100      . ",
            "          LXM,U     X0,022020    . ",
            "          LXSI,XU   X1,0711111   . ",
            "          LR,XU     R8,0755332   . ",
            "          LX,XU     X6,0777776   . ",
            "          LSBO,U    X6,035       . ",
            "          LX,U      X7,0444444   . ",
            "          LSBL,U    X7,033       . ",
            "          HALT      0            . ",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777776L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(02, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(03, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_777777_777773L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_777772L, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_000100_022020L, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0_111100_000000L, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
        assertEquals(0_777777_755332L, ip.getGeneralRegister(GeneralRegisterSet.R8).getW());
        assertEquals(0_357777_777776L, ip.getGeneralRegister(GeneralRegisterSet.X6).getW());
        assertEquals(0_003300_444444L, ip.getGeneralRegister(GeneralRegisterSet.X7).getW());
    }

    @Test
    public void X_A_RegisterOverlap(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  load some values into A0-A3, and check for corresponding values in X12-X15
        //  This will be extended mode.
        String[] source = {
            "          $EXTEND",
            "$(1)",
            "          LA,U      A0,01000",
            "          LA,U      A1,02000",
            "          LA,U      A2,03000",
            "          LA,U      A3,04000",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01000L, ip.getGeneralRegister(GeneralRegisterSet.X12).getW());
        assertEquals(02000L, ip.getGeneralRegister(GeneralRegisterSet.X13).getW());
        assertEquals(03000L, ip.getGeneralRegister(GeneralRegisterSet.X14).getW());
        assertEquals(04000L, ip.getGeneralRegister(GeneralRegisterSet.X15).getW());
    }

    @Test
    public void partialWordLoad_ThirdWordMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Tests the various third-word transfer modes in quarter-word-mode for load instructions
        //  This will be extended mode.
        String[] source = {
            "          $EXTEND",
            "$(0) .",
            "DATA_W     + 0112233445566          . for LA,W",
            "DATA_H2   + 0112233,0445566         . for LA,H2",
            "DATA_H1   + 0112233,0445566         . for LA,H1",
            "DATA_XH2P + 0111111,0377777         . for LA,XH2 positive operand",
            "DATA_XH2N + 0111111,0400000         . for LA,XH2 negative operand",
            "DATA_XH1P + 0355555,0222222         . for LA,XH1 positive operand",
            "DATA_XH1N + 0455555,0222222         . for LA,XH1 negative operand",
            "DATA_T3P  + 01111,02222,03333       . for LA,T3 positive operand",
            "DATA_T3N  + 02222,03333,06666       . for LA,T3 negative operand",
            "DATA_T2P  + 01111,02222,03333       . for LA,T2 positive operand",
            "DATA_T2N  + 02222,05555,01111       . for LA,T2 negative operand",
            "DATA_T1P  + 01111,02222,03333       . for LA,T1 positive operand",
            "DATA_T1N  + 04444,03333,02222       . for LA,T1 negative operand",

            "",
            "$(1) .",
            "          LA,W      A0,DATA_W,,B2",
            "          LA,H2     A1,DATA_H2,,B2",
            "          LA,H1     A2,DATA_H1,,B2",
            "          LA,XH2    A3,DATA_XH2P,,B2",
            "          LA,XH2    A4,DATA_XH2N,,B2",
            "          LA,XH1    A5,DATA_XH1P,,B2",
            "          LA,XH1    A6,DATA_XH1N,,B2",
            "          LA,T3     A7,DATA_T3P,,B2",
            "          LA,T3     A8,DATA_T3N,,B2",
            "          LA,T2     A9,DATA_T2P,,B2",
            "          LA,T2     A10,DATA_T2N,,B2",
            "          LA,T1     A11,DATA_T1P,,B2",
            "          LA,T1     A12,DATA_T1N,,B2",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0_112233_445566L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000000_445566L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_000000_112233L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_377777L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_777777_400000L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_355555L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_455555L, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0_000000_003333L, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_777777_776666L, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_000000_002222L, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_777777_775555L, ip.getGeneralRegister(GeneralRegisterSet.A10).getW());
        assertEquals(0_000000_001111L, ip.getGeneralRegister(GeneralRegisterSet.A11).getW());
        assertEquals(0_777777_774444L, ip.getGeneralRegister(GeneralRegisterSet.A12).getW());
    }

    @Test
    public void partialWordLoad_QuarterWordMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Tests the various partial-word transfer modes in quarter-word-mode for load instructions
        //  This will be extended mode.
        String[] source = {
            "          $EXTEND",
            "$(0)",
            "DATA_QW   + 0111,0222,0333,0444",
            "DATA_SW   + 011,022,033,044,055,066",
            "",
            "$(1)",
            "          LR,Q2     R0,DATA_QW,,B2",
            "          LR,Q4     R1,DATA_QW,,B2",
            "          LR,Q3     R2,DATA_QW,,B2",
            "          LR,Q1     R3,DATA_QW,,B2",
            "          LR,S6     R4,DATA_SW,,B2",
            "          LR,S5     R5,DATA_SW,,B2",
            "          LR,S4     R6,DATA_SW,,B2",
            "          LR,S3     R7,DATA_SW,,B2",
            "          LR,S2     R8,DATA_SW,,B2",
            "          LR,S1     R9,DATA_SW,,B2",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0222, ip.getGeneralRegister(GeneralRegisterSet.R0).getW());
        assertEquals(0444, ip.getGeneralRegister(GeneralRegisterSet.R1).getW());
        assertEquals(0333, ip.getGeneralRegister(GeneralRegisterSet.R2).getW());
        assertEquals(0111, ip.getGeneralRegister(GeneralRegisterSet.R3).getW());
        assertEquals(066, ip.getGeneralRegister(GeneralRegisterSet.R4).getW());
        assertEquals(055, ip.getGeneralRegister(GeneralRegisterSet.R5).getW());
        assertEquals(044, ip.getGeneralRegister(GeneralRegisterSet.R6).getW());
        assertEquals(033, ip.getGeneralRegister(GeneralRegisterSet.R7).getW());
        assertEquals(022, ip.getGeneralRegister(GeneralRegisterSet.R8).getW());
        assertEquals(011, ip.getGeneralRegister(GeneralRegisterSet.R9).getW());
    }

    //TODO need some various register-to-register loads

    //TODO need to test partial word x-fers (that they transfer the full word)

    @Test
    public void loadRegisterSet_normal(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        String[] source = {
            "          $EXTEND",
            "$(0),DATA .",
            "          + 1",
            "          + 2",
            "          + 3",
            "          + 4",
            "          + 5",
            "          + 6",
            "          + 7",
            "          + 8",
            "          + 9",
            "          + 10",
            "          + 11",
            "          + 12",
            "",
            "$(1),START*",
            "          LRS       A4,DATA,,B2",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        long count1 = 4;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 6;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;
        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.R0).getW());
        assertEquals(02, ip.getGeneralRegister(GeneralRegisterSet.R1).getW());
        assertEquals(03, ip.getGeneralRegister(GeneralRegisterSet.R2).getW());
        assertEquals(04, ip.getGeneralRegister(GeneralRegisterSet.R3).getW());
        assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(06, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(07, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());
        assertEquals(010, ip.getGeneralRegister(GeneralRegisterSet.X3).getW());
        assertEquals(011, ip.getGeneralRegister(GeneralRegisterSet.X4).getW());
        assertEquals(012, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void loadRegisterSet_count1Empty(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        String[] source = {
            "          $EXTEND",
            "$(0),DATA .",
            "          + 1",
            "          + 2",
            "          + 3",
            "          + 4",
            "          + 5",
            "          + 6",
            "          + 7",
            "          + 8",
            "          + 9",
            "          + 10",
            "          + 11",
            "          + 12",
            "",
            "$(1),START*",
            "          LRS       A4,DATA,,B2",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        long count1 = 0;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 6;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;
        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(02, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(03, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());
        assertEquals(04, ip.getGeneralRegister(GeneralRegisterSet.X3).getW());
        assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.X4).getW());
        assertEquals(06, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void loadRegisterSet_nop(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        String[] source = {
            "          $EXTEND",
            "$(0),DATA .",
            "          + 1",
            "          + 2",
            "          + 3",
            "          + 4",
            "          + 5",
            "          + 6",
            "          + 7",
            "          + 8",
            "          + 9",
            "          + 10",
            "          + 11",
            "          + 12",
            "",
            "$(1),START*",
            "          LRS       A4,DATA,,B2",
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        long count1 = 0;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 0;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;
        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.R0).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.R1).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.R2).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.R3).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X3).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X4).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void generalLoadFromStorage_Extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing load instructions which cannot be tested with immediate addressing
        //  Also, test loading from multiple banks, including exec banks
        String[] source = {
            "          $EXTEND",
            "",
            "$(0),DATA1 . Based on B2",
            "          + 0",
            "          + 0",
            "          + 01,01",
            "          + 01,02",
            "",
            "$(2),DATA2 . Based on B3",
            "          + 01,03",
            "          + 01,04",
            "          + 0777777777777",
            "          + 0777777777776",
            "",
            "$(4),DATA3 . Based on B4",
            "          + 01,05",
            "          + 01,06",
            "          + 0777777777777",
            "          + 0777777777775",
            "",
            "$(6),DATA4 . Based on B5",
            "          + 0112233,0445566",
            "          + 0223344,0556677",
            "",
            "$(8),DATA5 . Based on B6",
            "          + 0111,0222,0333,0444",
            "          + 0222,0333,0444,0555",
            "          + 0333,0444,0555,0666",
            "          + 0444,0555,0666,0777",
            "",
            "$(1),START* .",
            "          . Some special load instructions",
            "          DL        A0,DATA1+2,,B2",
            "          DLN       A2,DATA2,,B3",
            "          DLN       A4,DATA2+2,,B3",
            "          DLM       A6,DATA3,,B4",
            "          DLM       A8,DATA3+2,,B4",
            "          LXLM      X0,DATA4,,B5",
            "          LXSI      X0,DATA4+1,,B5",
            "",
            "          . Some LAQW Silliness",
            "          LXM,U     X5,0",
            "          LXI,U     X5,0",
            "          LAQW      A12,DATA5,X5,B6",
            "          LXM,U     X5,1",
            "          LXI,U     X5,010000",
            "          LAQW      A13,DATA5,X5,B6",
            "          LXM,U     X5,2",
            "          LXI,U     X5,020000",
            "          LAQW      A14,DATA5,X5,B6",
            "          LXM,U     X5,3",
            "          LXI,U     X5,030000",
            "          LAQW      A15,DATA5,X5,B6",
            "",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
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
        par.setProgramCounter(absoluteModule._entryPointAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01_000001, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(01_000002, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0777776_777774L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0777776_777773L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(1, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(01_000005, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(01_000006, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(2, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_667733_445566L, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0111, ip.getGeneralRegister(GeneralRegisterSet.A12).getW());
        assertEquals(0333, ip.getGeneralRegister(GeneralRegisterSet.A13).getW());
        assertEquals(0555, ip.getGeneralRegister(GeneralRegisterSet.A14).getW());
        assertEquals(0777, ip.getGeneralRegister(GeneralRegisterSet.A15).getW());
    }

    //TODO test generating various interrupts

    //TODO storage limits testing for load operand, store value, double load operand, LRS
}
