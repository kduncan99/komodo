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
public class Test_InstructionProcessor_Addressing_ExtendedMode extends Test_InstructionProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void immediateUnsigned_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "$(1),START",
            "          LA,U      A0,01000",
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

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(1),START",
                "          LA,XU     A0,01000",
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

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
                "          $EXTEND",
                "$(1),START",
                "          LA,XU     A0,0777777",
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

        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
                "          $EXTEND",
                "$(1),START",
                "          LA,XU     A0,-1",
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

        assertEquals(0_777777_777776L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "",
                "$(1),START",
                "          LR,U      R5,01234",
                "          LA        A0,R5",
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

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(2),DATA",
                "          01122,03344,05566",
                "",
                "$(1),START",
                "          LA        A0,DATA,,B2",
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

        assertEquals(0_112233_445566L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(1),START",
                "          LR,U      R5,01234",
                "          LXM,U     X1,4",
                "          LXI,U     X1,2",
                "          LA        A0,R1,*X1",
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

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006L, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void storage_indexed_18BitModifier_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(0)",
                "DATA1     0",
                "          01",
                "          0",
                "          0",
                "          02",
                "          0",
                "          0",
                "          03",
                "          0",
                "          0",
                "          05",
                "          0",
                "          0",
                "          010",
                "",
                "$(2),DATA2 . for auto-increment testing",
                "          $RES 8",
                "",
                "$(4),DATA3 . for X0 testing",
                "          $RES 8",
                "",
                "$(6),DATA4 . for non-auto-increment testing",
                "          $RES 8",
                "",
                "$(1),START",
                "          LXM,U     X5,1",
                "          LXI,U     X5,3",
                "          LXM,U     X7,0",
                "          LXI,U     X7,1",
                "          LXM,U     X0,1 . should do nothing",
                "          LXI,U     X0,1 . as above",
                "          LXM,U     X0,1",
                "          LXI,U     X1,1",
                "          LXM,U     X1,0",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          SA        A3,DATA3,*X0,B4",
                "          SA        A3,DATA4,*X1,B5",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          SA        A3,DATA3,*X0,B4",
                "          SA        A3,DATA4,*X1,B5",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          SA        A3,DATA3,*X0,B4",
                "          SA        A3,DATA4,*X1,B5",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          SA        A3,DATA3,*X0,B4",
                "          SA        A3,DATA4,*X1,B5",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          SA        A3,DATA3,*X0,B4",
                "          SA        A3,DATA4,*X1,B5",
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
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bank3Data = getBank(ip, 3);
        assertEquals(01, bank3Data[0]);
        assertEquals(02, bank3Data[1]);
        assertEquals(03, bank3Data[2]);
        assertEquals(05, bank3Data[3]);
        assertEquals(010, bank3Data[4]);

        long[] bank4Data = getBank(ip, 4);
        assertEquals(010, bank4Data[0]);
        assertEquals(0, bank4Data[1]);
        assertEquals(0, bank4Data[2]);
        assertEquals(0, bank4Data[3]);

        long[] bank5Data = getBank(ip, 4);
        assertEquals(010, bank5Data[0]);
        assertEquals(0, bank5Data[1]);
        assertEquals(0, bank5Data[2]);
        assertEquals(0, bank5Data[3]);
    }

    @Test
    public void storage_indexed_24BitModifier_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(0)",
                "DATA1     0",
                "          01",
                "          0",
                "          0",
                "          02",
                "          0",
                "          0",
                "          03",
                "          0",
                "          0",
                "          05",
                "          0",
                "          0",
                "          010",
                "",
                "$(2),DATA2",
                "          $RES 8",
                "",
                "$(1),START",
                "          LXM,U     X5,1",
                "          LXI,U     X5,0300",
                "          LXM,U     X7,0",
                "          LXI,U     X7,0100",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
                "          LA        A3,DATA1,*X5,B2",
                "          SA        A3,DATA2,*X7,B3",
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
        dReg.setExecutive24BitIndexingEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 3);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void execRegisterSelection_ExtendedMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "$(1),START",
                "          LA,U      EA5,01",
                "          LX,U      EX5,05",
                "          LR,U      ER5,077",
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
        dReg.setExecRegisterSetSelected(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }

    //TODO read reference violation GAP

    //TODO write reference violation GAP

    //TODO execute reference violation GAP

    //TODO read reference violation SAP

    //TODO write reference violation SAP

    //TODO execute reference violation SAP

    //TODO reference out of limits EXTENDED mode

    //TODO unbased Base Register reference EXTENDED mode

}
