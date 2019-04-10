/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_GeneralLoadInstructions extends Test_InstructionProcessor {


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Instruction Execution, Addressing Modes, and such
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void generalLoadImmediate_Extended(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Write some load instructions starting at absolute address 01000 for the MSP's UPI
        //  This will be extended mode.
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 01000)).getW(),        //  LA,U    A0,01000
            (new InstructionWord(011, 016, 1, 0, 01)).getW(),           //  LNA,U   A1,1
            (new InstructionWord(011, 017, 2, 0, 0777776)).getW(),      //  LNA,XU  A2,-1
            (new InstructionWord(012, 016, 3, 0, 2)).getW(),            //  LMA,U   A3,2
            (new InstructionWord(012, 017, 4, 0, 0777774)).getW(),      //  LMA,XU  A4,-3
            (new InstructionWord(013, 016, 5, 0, 4)).getW(),            //  LNMA,U  A5,4
            (new InstructionWord(013, 017, 6, 0, 0777772)).getW(),      //  LNMA,XU A6,-5
            (new InstructionWord(010, 017, 7, 0, 0777777)).getW(),      //  LA,XU   A7,0777777  - make sure neg zero is eliminated
            (new InstructionWord(027, 017, 5, 0, 01234)).getW(),        //  LX,XU   X5,01234
            (new InstructionWord(046, 016, 0, 0, 0100)).getW(),         //  LXI,U   X0,0100
            (new InstructionWord(026, 016, 0, 0, 022020)).getW(),       //  LXM,U   X0,022020
            (new InstructionWord(051, 017, 1, 0, 0711111)).getW(),      //  LXSI,XU X1,0711111
            (new InstructionWord(023, 017, 010, 0, 0755332)).getW(),    //  LR,XU   R8,0755332
            (new InstructionWord(027, 017, 06, 0, 0777776)).getW(),     //  L,XU    X6,0777776
            (new InstructionWord(060, 016, 06, 0, 035)).getW(),         //  LSBO,U  X6,035
            (new InstructionWord(027, 016, 07, 0, 0444444)).getW(),     //  L,U     X7,0444444
            (new InstructionWord(061, 016, 07, 0, 033)).getW(),         //  LSBL,U  X7,033
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
        assertEquals(0_777777_777776l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(02, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(03, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_777777_777773l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_777772l, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_000100_022020l, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0_111100_000000l, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
        assertEquals(0_777777_755332l, ip.getGeneralRegister(GeneralRegisterSet.R8).getW());
        assertEquals(0_357777_777776l, ip.getGeneralRegister(GeneralRegisterSet.X6).getW());
        assertEquals(0_003300_444444l, ip.getGeneralRegister(GeneralRegisterSet.X7).getW());
        assertEquals(InstructionProcessor.StopReason.InitiateAutoRecovery, ip.getLatestStopReason());
    }

    @Test
    public void X_A_RegisterOverlap(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  load some values into A0-A3, and check for corresponding values in X12-X15
        //  This will be extended mode.
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 01000)).getW(),        //  LA,U    A0,01000
            (new InstructionWord(010, 016, 1, 0, 02000)).getW(),        //  LA,U    A1,02000
            (new InstructionWord(010, 016, 2, 0, 03000)).getW(),        //  LA,U    A2,03000
            (new InstructionWord(010, 016, 3, 0, 04000)).getW(),        //  LA,U    A3,04000
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

        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.X12).getW());
        assertEquals(02000, ip.getGeneralRegister(GeneralRegisterSet.X13).getW());
        assertEquals(03000, ip.getGeneralRegister(GeneralRegisterSet.X14).getW());
        assertEquals(04000, ip.getGeneralRegister(GeneralRegisterSet.X15).getW());
        assertEquals(InstructionProcessor.StopReason.InitiateAutoRecovery, ip.getLatestStopReason());
    }

    @Test
    public void partialWordLoad_ThirdWordMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Tests the various partial-word transfer modes in third-word-mode for load instructions
        //  This will be extended mode.
        long[] data = {
            (new Word36(0_112233_445566l)).getW(),  //  For LA,W
            (new Word36(0_112233_445566l)).getW(),  //  For LA,H2
            (new Word36(0_112233_445566l)).getW(),  //  For LA,H1
            (new Word36(0_111111_377777l)).getW(),  //  For LA,XH2 pos
            (new Word36(0_111111_400000l)).getW(),  //  For LA,XH2 neg
            (new Word36(0_355555_222222l)).getW(),  //  For LA,XH1 pos
            (new Word36(0_455555_222222l)).getW(),  //  For LA,XH1 neg
            (new Word36(0_1111_2222_3333l)).getW(), //  For LA,T3 pos
            (new Word36(0_4444_5555_6666l)).getW(), //  For LA,T3 neg
            (new Word36(0_1111_2222_3333l)).getW(), //  For LA,T2 pos
            (new Word36(0_4444_5555_6666l)).getW(), //  For LA,T2 neg
            (new Word36(0_1111_2222_3333l)).getW(), //  For LA,T1 pos
            (new Word36(0_4444_5555_6666l)).getW(), //  For LA,T1 neg
        };

        long[] code = {
            (new InstructionWord(010, 000, 000, 0, 0, 0, 1, 000)).getW(),   //  LA,W    A0,00,B1
            (new InstructionWord(010, 001, 001, 0, 0, 0, 1, 001)).getW(),   //  LA,H2   A1,01,B1
            (new InstructionWord(010, 002, 002, 0, 0, 0, 1, 002)).getW(),   //  LA,H1   A2,02,B1
            (new InstructionWord(010, 003, 003, 0, 0, 0, 1, 003)).getW(),   //  LA,XH2  A3,03,B1
            (new InstructionWord(010, 003, 004, 0, 0, 0, 1, 004)).getW(),   //  LA,XH2  A4,04,B1
            (new InstructionWord(010, 004, 005, 0, 0, 0, 1, 005)).getW(),   //  LA,XH1  A5,05,B1
            (new InstructionWord(010, 004, 006, 0, 0, 0, 1, 006)).getW(),   //  LA,XH1  A6,06,B1
            (new InstructionWord(010, 005, 007, 0, 0, 0, 1, 007)).getW(),   //  LA,T3   A7,07,B1
            (new InstructionWord(010, 005, 010, 0, 0, 0, 1, 010)).getW(),   //  LA,T3   A8,010,B1
            (new InstructionWord(010, 006, 011, 0, 0, 0, 1, 011)).getW(),   //  LA,T2   A9,07,B1
            (new InstructionWord(010, 006, 012, 0, 0, 0, 1, 012)).getW(),   //  LA,T2   A10,010,B1
            (new InstructionWord(010, 007, 013, 0, 0, 0, 1, 013)).getW(),   //  LA,T1   A11,07,B1
            (new InstructionWord(010, 007, 014, 0, 0, 0, 1, 014)).getW(),   //  LA,T1   A12,010,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_112233_445566l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000000_445566l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_000000_112233l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_377777l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_777777_400000l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_355555l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_455555l, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0_000000_003333l, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_777777_776666l, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_000000_002222l, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_777777_775555l, ip.getGeneralRegister(GeneralRegisterSet.A10).getW());
        assertEquals(0_000000_001111l, ip.getGeneralRegister(GeneralRegisterSet.A11).getW());
        assertEquals(0_777777_774444l, ip.getGeneralRegister(GeneralRegisterSet.A12).getW());
        assertEquals(InstructionProcessor.StopReason.InitiateAutoRecovery, ip.getLatestStopReason());
    }

    @Test
    public void partialWordLoad_QuarterWordMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Tests the various partial-word transfer modes in quarter-word-mode for load instructions
        //  This will be extended mode.
        long[] data = {
            (new Word36(0_111222_333444l)).getW(),  //  For LA,Q2
            (new Word36(0_111222_333444l)).getW(),  //  For LA,Q4
            (new Word36(0_111222_333444l)).getW(),  //  For LA,Q3
            (new Word36(0_111222_333444l)).getW(),  //  For LA,Q1
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S6
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S5
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S4
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S3
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S2
            (new Word36(0_112233_445566l)).getW(),  //  For LA,S1
        };

        long[] code = {
            (new InstructionWord(023, 004, 000, 0, 0, 0, 1, 000)).getW(),   //  LR,Q2   R0,000,B1
            (new InstructionWord(023, 005, 001, 0, 0, 0, 1, 001)).getW(),   //  LR,Q4   R1,001,B1
            (new InstructionWord(023, 006, 002, 0, 0, 0, 1, 002)).getW(),   //  LR,Q3   R2,002,B1
            (new InstructionWord(023, 007, 003, 0, 0, 0, 1, 003)).getW(),   //  LR,Q1   R3,003,B1
            (new InstructionWord(023, 010, 004, 0, 0, 0, 1, 004)).getW(),   //  LR,S6   R4,004,B1
            (new InstructionWord(023, 011, 005, 0, 0, 0, 1, 005)).getW(),   //  LR,S5   R4,005,B1
            (new InstructionWord(023, 012, 006, 0, 0, 0, 1, 006)).getW(),   //  LR,S4   R4,006,B1
            (new InstructionWord(023, 013, 007, 0, 0, 0, 1, 007)).getW(),   //  LR,S3   R4,007,B1
            (new InstructionWord(023, 014, 010, 0, 0, 0, 1, 010)).getW(),   //  LR,S2   R4,010,B1
            (new InstructionWord(023, 015, 011, 0, 0, 0, 1, 011)).getW(),   //  LR,S1   R4,011,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
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
        assertEquals(InstructionProcessor.StopReason.InitiateAutoRecovery, ip.getLatestStopReason());
    }

    @Test
    public void generalLoadFromGRS_Extended(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //???? some various register-to-register loads
        //???? need to test partial word x-fers (that they transfer the full word)
    }

    @Test
    public void loadRegisterSet_normal(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        long data[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        long code[] = {
            (new InstructionWord(072, 017, 04, 0, 0, 0, 1, 0)).getW(),      //  LRS     A4,0,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        long count1 = 4;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 6;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        long data[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        long code[] = {
            (new InstructionWord(072, 017, 04, 0, 0, 0, 1, 0)).getW(),      //  LRS     A4,0,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        long count1 = 0;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 6;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing LRS with non-zero count1 and count2
        long data[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
        long code[] = {
            (new InstructionWord(072, 017, 04, 0, 0, 0, 1, 0)).getW(),      //  LRS     A4,0,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        long count1 = 0;
        long area1 = GeneralRegisterSet.R0;
        long count2 = 0;
        long area2 = GeneralRegisterSet.X0;
        long descriptor = (count2 << 27) | (area2 << 18) | (count1 << 9) | area1;

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        ip.setGeneralRegister(GeneralRegisterSet.A4, descriptor);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Testing load instructions which cannot be tested with immediate addressing
        //  Also, test loading from multiple banks, including exec banks
        long data1[] = {
            0,
            0,
            01_000001,
            01_000002,
        };

        long data2[] = {
            01_000003,
            01_000004,
            0_777777_777777l,
            0_777777_777776l,
        };

        long data3[] = {
            01_000005,
            01_000006,
            0_777777_777777l,
            0_777777_777775l,
        };

        long data4[] = {
            0_112233_445566l,
            0_223344_556677l,
        };

        long data5[] = {
            0_111_222_333_444l,
            0_222_333_444_555l,
            0_333_444_555_666l,
            0_444_555_666_777l,
        };

        long code[] = {
            (new InstructionWord(071, 013, 0, 0, 0, 0, 1, 2)).getW(),   //  DL      A0,2,,B1
            (new InstructionWord(071, 014, 2, 0, 0, 0, 2, 0)).getW(),   //  DLN     A2,0,,B2
            (new InstructionWord(071, 014, 4, 0, 0, 0, 2, 2)).getW(),   //  DLN     A4,2,,B2
            (new InstructionWord(071, 015, 6, 0, 0, 0, 3, 0)).getW(),   //  DLM     A6,0,,B3
            (new InstructionWord(071, 015, 8, 0, 0, 0, 3, 2)).getW(),   //  DLM     A8,2,,B3
            (new InstructionWord(075, 013, 0, 0, 0, 0, 4, 0)).getW(),   //  LXLM    X0,0,,B4
            (new InstructionWord(051, 0,   0, 0, 0, 0, 4, 1)).getW(),   //  LXSI    X0,1,,B4

            //  Some LAQW tomfoolery
            (new InstructionWord(026, 016, 5, 0, 0)).getW(),            //  LXM,U   X5,0
            (new InstructionWord(046, 016, 5, 0, 0)).getW(),            //  LXI,U   X5,0
            (new InstructionWord(007, 04, 12, 5, 0, 0, 5, 0)).getW(),   //  LAQW    A12,0,X5,B5
            (new InstructionWord(026, 016, 5, 0, 1)).getW(),            //  LXM,U   X5,1
            (new InstructionWord(046, 016, 5, 0, 010000)).getW(),       //  LXI,U   X5,010000
            (new InstructionWord(007, 04, 13, 5, 0, 0, 5, 0)).getW(),   //  LAQW    A13,0,X5,B5
            (new InstructionWord(026, 016, 5, 0, 2)).getW(),            //  LXM,U   X5,2
            (new InstructionWord(046, 016, 5, 0, 020000)).getW(),       //  LXI,U   X5,020000
            (new InstructionWord(007, 04, 14, 5, 0, 0, 5, 0)).getW(),   //  LAQW    A14,0,X5,B5
            (new InstructionWord(026, 016, 5, 0, 3)).getW(),            //  LXM,U   X5,3
            (new InstructionWord(046, 016, 5, 0, 030000)).getW(),       //  LXI,U   X5,030000
            (new InstructionWord(007, 04, 15, 5, 0, 0, 5, 0)).getW(),   //  LAQW    A15,0,X5,B5

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data1, data2, data3, data4, data5 };

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

        assertEquals(01_000001, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(01_000002, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0777776_777774l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0777776_777773l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(1, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(01_000005, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(01_000006, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(2, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_667733_445566l, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0111, ip.getGeneralRegister(GeneralRegisterSet.A12).getW());
        assertEquals(0333, ip.getGeneralRegister(GeneralRegisterSet.A13).getW());
        assertEquals(0555, ip.getGeneralRegister(GeneralRegisterSet.A14).getW());
        assertEquals(0777, ip.getGeneralRegister(GeneralRegisterSet.A15).getW());
    }

    //???? test generating various interrupts

    //???? storage limits testing for load operand, store value, double load operand, LRS
}
