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
public class Test_InstructionProcessor_Addressing extends Test_InstructionProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    //???? somehow we missed the fact that we always increment X-reg even with H-bit clear
    //         figure out why we missed that, and add another test if necessary

    @Test
    public void immediateUnsigned_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          LA,U      A0,01000",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          LA,XU     A0,01000",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
                "          LA,XU     A0,0777777",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          LA,XU     A0,-1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777776l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          LR,U      R5,01234",
                "          LA        A0,R5",
                "          HALT      0"
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {

                "          LR,U      R5,01234    . Put the test value in R5",
                "          LXM,U     X1,4        . Set X modifier to 4 and increment to 2",
                "          LXI,U     X1,2",
                "          LA        A0,R1,*X1   . Use X-reg modifying R1 GRS to get to R5",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(022000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006l, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void grs_indirect_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            (new InstructionWord(0, 0, 0, 0, 0, 0, GeneralRegisterSet.R5)).getW(),      //  Reference to GRS
        };

        LoadBankInfo dataInfo = new LoadBankInfo(data);
        dataInfo._lowerLimit = 022000;

        long[] code = {
            (new InstructionWord(023, 016, 5, 0, 01234)).getW(),            //  LR,U    R5,01234
            (new InstructionWord(010, 0, 0, 0, 0, 1, 022000)).getW(),       //  LA      A0,*022000 . indirect through 022000
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo, dataInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_indexed_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data1 = { 01, 0, 0, 02, 0, 0, 03, 0, 0, 05, 0, 0, 010 };
        LoadBankInfo data1Info = new LoadBankInfo(data1);
        data1Info._lowerLimit = 022000;

        long[] data2 = { 0, 0, 0, 0, 0, 0, 0, 0, };
        LoadBankInfo data2Info = new LoadBankInfo(data2);
        data2Info._lowerLimit = 050000;

        long[] code = {
            (new InstructionWord(026, 016, 5, 0, 01)).getW(),           //  LXM,U   X5,1
            (new InstructionWord(046, 016, 5, 0, 03)).getW(),           //  LXI,U   X5,3
            (new InstructionWord(026, 016, 7, 0, 00)).getW(),           //  LXM,U   X7,0
            (new InstructionWord(046, 016, 7, 0, 01)).getW(),           //  LXI,U   X7,1
            (new InstructionWord(010, 0, 3, 5, 1, 0, 021777)).getW(),   //  LA      A3,021777,*X5
            (new InstructionWord(001, 0, 3, 7, 1, 0, 050000)).getW(),   //  SA      A3,050000,*X7
            (new InstructionWord(010, 0, 3, 5, 1, 0, 021777)).getW(),   //  LA      A3,021777,*X5
            (new InstructionWord(001, 0, 3, 7, 1, 0, 050000)).getW(),   //  SA      A3,050000,*X7
            (new InstructionWord(010, 0, 3, 5, 1, 0, 021777)).getW(),   //  LA      A3,021777,*X5
            (new InstructionWord(001, 0, 3, 7, 1, 0, 050000)).getW(),   //  SA      A3,050000,*X7
            (new InstructionWord(010, 0, 3, 5, 1, 0, 021777)).getW(),   //  LA      A3,021777,*X5
            (new InstructionWord(001, 0, 3, 7, 1, 0, 050000)).getW(),   //  SA      A3,050000,*X7
            (new InstructionWord(010, 0, 3, 5, 1, 0, 021777)).getW(),   //  LA      A3,021777,*X5
            (new InstructionWord(001, 0, 3, 7, 1, 0, 050000)).getW(),   //  SA      A3,050000,*X7
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };
        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo, data1Info, data2Info };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 14);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void storage_indirect_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data1 = {
            (new InstructionWord(0, 0, 0, 0, 0, 1, 050000)).getW(),     //  indirect to 050000 in databank2
            (new InstructionWord(0, 0, 0, 0, 0, 1, 022002)).getW(),     //  indirect to 022002
            (new InstructionWord(0, 0, 0, 0, 0, 1, 022003)).getW(),     //  indirect to 022003
            (new InstructionWord(0, 0, 0, 0, 0, 1, 022004)).getW(),     //  indirect to 022004
            (new InstructionWord(0, 0, 0, 0, 0, 0, 050001)).getW(),     //  indirect to 050001 in databank2
        };

        LoadBankInfo data1Info = new LoadBankInfo(data1);
        data1Info._lowerLimit = 022000;
        long[] data2 = {
            (new InstructionWord(0, 0, 0, 0, 0, 1, 022001)).getW(),     //  indirect to 022001 in databank1
            0112233_445566l,                                            //  the actual data
        };

        LoadBankInfo data2Info = new LoadBankInfo(data2);
        data2Info._lowerLimit = 050000;

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 1, 022000)).getW(),       //  LA      A0,*022000 . indirect through 022000
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo, data1Info, data2Info };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_112233_445566l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void execRegisterSelection_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 05, 0, 01)).getW(),          //  LA,U    EA5,01
            (new InstructionWord(027, 016, 05, 0, 05)).getW(),          //  LX,U    EX5,05
            (new InstructionWord(023, 016, 05, 0, 077)).getW(),         //  LR,U    ER5,077
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);
        dReg.setExecRegisterSetSelected(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }

    @Test
    public void storage_BasicMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_112233_445566l,
        };
        LoadBankInfo dataInfo = new LoadBankInfo(data);
        dataInfo._lowerLimit = 022000;

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 022000)).getW(),   //  LA      A0,022000
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo, dataInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_112233_445566l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateUnsigned_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 01000)).getW(),        //  LA,U    A0,01000
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 01000)).getW(),        //  LA,XU   A0,01000
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 0777777)).getW(),      //  LA,XU   A0,0777777
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 0777776)).getW(),      //  LA,XU   A0,-1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777776l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_112233_445566l,
        };
        LoadBankInfo dataInfo = new LoadBankInfo(data);
        dataInfo._lowerLimit = 022000;

        long[] code = {
            (new InstructionWord(023, 016, 5, 0, 01234)).getW(),                        //  LR,U    R5,01234
            (new InstructionWord(010, 0, 0, 0, 0, 0, GeneralRegisterSet.R5)).getW(),    //  LA      A0,R5
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),                  //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo, dataInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_112233_445566l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_112233_445566l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(023, 016, 5, 0, 01234)).getW(),                        //  LR,U    R5,01234
            (new InstructionWord(026, 016, 1, 0, 04)).getW(),                           //  LXM,U   X1,4
            (new InstructionWord(046, 016, 1, 0, 02)).getW(),                           //  LXI,U   X1,2
            (new InstructionWord(010, 0, 0, 1, 1, 0, GeneralRegisterSet.R1)).getW(),    //  LA      A0,R1,*X1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),                  //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006l, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void storage_indexed_18BitModifier_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data1 = { 01, 0, 0, 02, 0, 0, 03, 0, 0, 05, 0, 0, 010 };
        LoadBankInfo data1Info = new LoadBankInfo(data1);
        data1Info._lowerLimit = 02000;

        long[] data2 = { 0, 0, 0, 0, 0, 0, 0, 0, };
        LoadBankInfo data2Info = new LoadBankInfo(data2);
        data2Info._lowerLimit = 02000;

        long[] code = {
            (new InstructionWord(026, 016, 5, 0, 01)).getW(),           //  LXM,U   X5,1
            (new InstructionWord(046, 016, 5, 0, 03)).getW(),           //  LXI,U   X5,3
            (new InstructionWord(026, 016, 7, 0, 00)).getW(),           //  LXM,U   X7,0
            (new InstructionWord(046, 016, 7, 0, 01)).getW(),           //  LXI,U   X7,1
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };
        LoadBankInfo codeInfo = new LoadBankInfo(code);

        LoadBankInfo infos[] = { codeInfo, data1Info, data2Info };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void storage_indexed_24BitModifier_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data1 = { 01, 0, 0, 02, 0, 0, 03, 0, 0, 05, 0, 0, 010 };
        LoadBankInfo data1Info = new LoadBankInfo(data1);
        data1Info._lowerLimit = 02000;

        long[] data2 = { 0, 0, 0, 0, 0, 0, 0, 0, };
        LoadBankInfo data2Info = new LoadBankInfo(data2);
        data2Info._lowerLimit = 02000;

        long[] code = {
            (new InstructionWord(026, 016, 5, 0, 01)).getW(),           //  LXM,U   X5,1
            (new InstructionWord(046, 016, 5, 0, 0300)).getW(),         //  LXI,U   X5,0300
            (new InstructionWord(026, 016, 7, 0, 00)).getW(),           //  LXM,U   X7,0
            (new InstructionWord(046, 016, 7, 0, 0100)).getW(),         //  LXI,U   X7,0100
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(010, 0, 3, 5, 1, 0, 1, 01777)).getW(), //  LA      A3,01777,*X5,B1
            (new InstructionWord(001, 0, 3, 7, 1, 0, 2, 02000)).getW(), //  SA      A3,02000,*X7,B2
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };
        LoadBankInfo codeInfo = new LoadBankInfo(code);

        LoadBankInfo infos[] = { codeInfo, data1Info, data2Info };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setExecutive24BitIndexingEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 2);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void execRegisterSelection_ExtendedMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 05, 0, 01)).getW(),          //  LA,U    EA5,01
            (new InstructionWord(027, 016, 05, 0, 05)).getW(),          //  LX,U    EX5,05
            (new InstructionWord(023, 016, 05, 0, 077)).getW(),         //  LR,U    ER5,077
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        LoadBankInfo infos[] = { codeInfo };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);
        dReg.setExecRegisterSetSelected(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }
}
