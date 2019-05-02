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
public class Test_InstructionProcessor_GeneralStoreInstructions extends Test_InstructionProcessor{


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Instruction Execution, Addressing Modes, and such
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void generalStore_PartialWords_QuarterWordMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = new long[32];
        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 0444444)).getW(),      //  LA,XU   A0,0444444
            (new InstructionWord(001, 000, 0, 0, 0, 0, 1, 0)).getW(),   //  SA,W    A0,0,,B1
            (new InstructionWord(001, 001, 0, 0, 0, 0, 1, 1)).getW(),   //  SA,H2   A0,1,,B1
            (new InstructionWord(001, 002, 0, 0, 0, 0, 1, 2)).getW(),   //  SA,H1   A0,2,,B1
            (new InstructionWord(001, 003, 0, 0, 0, 0, 1, 3)).getW(),   //  SA,XH2  A0,3,,B1
            (new InstructionWord(001, 004, 0, 0, 0, 0, 1, 4)).getW(),   //  SA,Q2   A0,4,,B1
            (new InstructionWord(001, 005, 0, 0, 0, 0, 1, 5)).getW(),   //  SA,Q4   A0,5,,B1
            (new InstructionWord(001, 006, 0, 0, 0, 0, 1, 6)).getW(),   //  SA,Q3   A0,6,,B1
            (new InstructionWord(001, 007, 0, 0, 0, 0, 1, 7)).getW(),   //  SA,Q1   A0,7,,B1
            (new InstructionWord(001, 010, 0, 0, 0, 0, 1, 8)).getW(),   //  SA,S6   A0,010,,B1
            (new InstructionWord(001, 011, 0, 0, 0, 0, 1, 9)).getW(),   //  SA,S5   A0,011,,B1
            (new InstructionWord(001, 012, 0, 0, 0, 0, 1, 10)).getW(),  //  SA,S4   A0,012,,B1
            (new InstructionWord(001, 013, 0, 0, 0, 0, 1, 11)).getW(),  //  SA,S3   A0,013,,B1
            (new InstructionWord(001, 014, 0, 0, 0, 0, 1, 12)).getW(),  //  SA,S2   A0,014,,B1
            (new InstructionWord(001, 015, 0, 0, 0, 0, 1, 13)).getW(),  //  SA,S1   A0,015,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_777777_444444l, bankData[0]);
        assertEquals(0_000000_444444l, bankData[1]);
        assertEquals(0_444444_000000l, bankData[2]);
        assertEquals(0_000000_444444l, bankData[3]);
        assertEquals(0_000444_000000l, bankData[4]);
        assertEquals(0_000000_000444l, bankData[5]);
        assertEquals(0_000000_444000l, bankData[6]);
        assertEquals(0_444000_000000l, bankData[7]);
        assertEquals(0_000000_000044l, bankData[8]);
        assertEquals(0_000000_004400l, bankData[9]);
        assertEquals(0_000000_440000l, bankData[10]);
        assertEquals(0_000044_000000l, bankData[11]);
        assertEquals(0_004400_000000l, bankData[12]);
        assertEquals(0_440000_000000l, bankData[13]);
    }

    @Test
    public void generalStore_PartialWords_ThirdWordMode(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = new long[32];
        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 0444444)).getW(),      //  LA,XU   A0,0444444
            (new InstructionWord(001, 004, 0, 0, 0, 0, 1, 0)).getW(),   //  SA,XH1  A0,0,,B1
            (new InstructionWord(001, 005, 0, 0, 0, 0, 1, 1)).getW(),   //  SA,T3   A0,1,,B1
            (new InstructionWord(001, 006, 0, 0, 0, 0, 1, 2)).getW(),   //  SA,T2   A0,2,,B1
            (new InstructionWord(001, 007, 0, 0, 0, 0, 1, 3)).getW(),   //  SA,T1   A0,3,,B1
            (new InstructionWord(001, 017, 0, 0, 0, 0, 1, 3)).getW(),   //  SA,XU   A0,5,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_444444_000000l, bankData[0]);
        assertEquals(0_000000_004444l, bankData[1]);
        assertEquals(0_000044_440000l, bankData[2]);
        assertEquals(0_444400_000000l, bankData[3]);
        assertEquals(0_000000_000000l, bankData[5]);
    }

    @Test
    public void generalStore(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = new long[32];

        long[] code = {
            (new InstructionWord(010, 017, 0, 0, 0444444)).getW(),      //  LA,XU   A0,0444444
            (new InstructionWord(010, 016, 1, 0, 0444444)).getW(),      //  LA,U    A1,0444444
            (new InstructionWord(023, 016, 2, 0, 010110)).getW(),       //  LR,U    R2,010110
            (new InstructionWord(027, 016, 5, 0, 020220)).getW(),       //  LR,U    X5,020220
            (new InstructionWord(002, 000, 0, 0, 0, 0, 1, 0)).getW(),   //  SNA     A0,0,,B1
            (new InstructionWord(002, 000, 1, 0, 0, 0, 1, 1)).getW(),   //  SNA     A1,1,,B1
            (new InstructionWord(003, 000, 0, 0, 0, 0, 1, 2)).getW(),   //  SMA     A0,2,,B1
            (new InstructionWord(003, 000, 1, 0, 0, 0, 1, 3)).getW(),   //  SMA     A1,3,,B1
            (new InstructionWord(004, 000, 2, 0, 0, 0, 1, 4)).getW(),   //  SR      R2,4,,B1
            (new InstructionWord(006, 000, 5, 0, 0, 0, 1, 5)).getW(),   //  SX      X5,5,,B1
            (new InstructionWord(071, 012, 0, 0, 0, 0, 1, 6)).getW(),   //  DS      A0,6,,B1

            (new InstructionWord(046, 016, 5, 0, 0_000000)).getW(),     //  LXI,U   X5,0_000000
            (new InstructionWord(026, 016, 5, 0, 0_000000)).getW(),     //  LXM,U   X5,0_000000
            (new InstructionWord(007, 005, 1, 5, 0, 0, 1, 16)).getW(),  //  SAQW    A1,16,X5,B1
            (new InstructionWord(046, 016, 5, 0, 0_010000)).getW(),     //  LXI,U   X5,0_010000
            (new InstructionWord(026, 016, 5, 0, 0_000001)).getW(),     //  LXM,U   X5,0_000001
            (new InstructionWord(007, 005, 1, 5, 0, 0, 1, 16)).getW(),  //  SAQW    A1,16,X5,B1
            (new InstructionWord(046, 016, 5, 0, 0_020000)).getW(),     //  LXI,U   X5,0_020000
            (new InstructionWord(026, 016, 5, 0, 0_000002)).getW(),     //  LXM,U   X5,0_000002
            (new InstructionWord(007, 005, 1, 5, 0, 0, 1, 16)).getW(),  //  SAQW    A1,16,X5,B1
            (new InstructionWord(046, 016, 5, 0, 0_030000)).getW(),     //  LXI,U   X5,0_030000
            (new InstructionWord(026, 016, 5, 0, 0_000003)).getW(),     //  LXM,U   X5,0_000003
            (new InstructionWord(007, 005, 1, 5, 0, 0, 1, 16)).getW(),  //  SAQW    A1,16,X5,B1

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_333333l, bankData[0]);
        assertEquals(0_777777_333333l, bankData[1]);
        assertEquals(0_000000_333333l, bankData[2]);
        assertEquals(0_000000_444444l, bankData[3]);
        assertEquals(0_000000_010110l, bankData[4]);
        assertEquals(0_000000_020220l, bankData[5]);
        assertEquals(0_777777_444444l, bankData[6]);
        assertEquals(0_000000_444444l, bankData[7]);
        assertEquals(0_444000_000000l, bankData[16]);
        assertEquals(0_000444_000000l, bankData[17]);
        assertEquals(0_000000_444000l, bankData[18]);
        assertEquals(0_000000_000444l, bankData[19]);
    }

    @Test
    public void generalStore_FixedValues(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = new long[32];
        for (int dx = 0; dx < data.length; ++dx) {
            data[dx] = 0_343434_343434l;
        }

        long[] code = {
            (new InstructionWord(005, 000, 0, 0, 0, 0, 1, 0)).getW(),   //  SZ      0,,B1
            (new InstructionWord(005, 000, 1, 0, 0, 0, 1, 1)).getW(),   //  SNZ     1,,B1
            (new InstructionWord(005, 002, 2, 0, 0, 0, 1, 2)).getW(),   //  SP1,H1  2,,B1
            (new InstructionWord(005, 000, 3, 0, 0, 0, 1, 3)).getW(),   //  SN1     3,,B1
            (new InstructionWord(005, 001, 4, 0, 0, 0, 1, 4)).getW(),   //  SFS,H2  4,,B1
            (new InstructionWord(005, 000, 5, 0, 0, 0, 1, 5)).getW(),   //  SFZ     5,,B1
            (new InstructionWord(005, 000, 6, 0, 0, 0, 1, 6)).getW(),   //  SAS     6,,B1
            (new InstructionWord(005, 000, 7, 0, 0, 0, 1, 7)).getW(),   //  SAZ     7,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000000l, bankData[0]);
        assertEquals(0_777777_777777l, bankData[1]);
        assertEquals(0_000001_343434l, bankData[2]);
        assertEquals(0_777777_777776l, bankData[3]);
        assertEquals(0_343434_050505l, bankData[4]);
        assertEquals(0_606060_606060l, bankData[5]);
        assertEquals(0_040040_040040l, bankData[6]);
        assertEquals(0_060060_060060l, bankData[7]);
    }

    @Test
    public void storeRegisterSet(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  Set up descriptor to transfer 5 words from R0:R4, and 3 words from A3:A5
        long address1 = GeneralRegisterSet.R0;
        long count1 = 5;
        long address2 = GeneralRegisterSet.A3;
        long count2 = 3;
        long descriptor = (count2 << 27) | (address2 << 18) | (count1 << 9) | address1;

        long[] data1 = new long[10];
        long[] data2 = { descriptor };
        long[] code = {
            //  set up registers
            (new InstructionWord(010, 016, 0, 0, 001)).getW(),  //  LA,U    A0,1
            (new InstructionWord(010, 016, 1, 0, 002)).getW(),  //  LA,U    A1,2
            (new InstructionWord(010, 016, 2, 0, 003)).getW(),  //  LA,U    A2,3
            (new InstructionWord(010, 016, 3, 0, 004)).getW(),  //  LA,U    A3,4
            (new InstructionWord(010, 016, 4, 0, 005)).getW(),  //  LA,U    A4,5
            (new InstructionWord(010, 016, 5, 0, 006)).getW(),  //  LA,U    A5,6
            (new InstructionWord(010, 016, 6, 0, 007)).getW(),  //  LA,U    A6,7
            (new InstructionWord(010, 016, 7, 0, 010)).getW(),  //  LA,U    A7,010
            (new InstructionWord(023, 016, 0, 0, 021)).getW(),  //  LR,U    R0,021
            (new InstructionWord(023, 016, 1, 0, 031)).getW(),  //  LR,U    R1,031
            (new InstructionWord(023, 016, 2, 0, 041)).getW(),  //  LR,U    R2,041
            (new InstructionWord(023, 016, 3, 0, 051)).getW(),  //  LR,U    R3,051
            (new InstructionWord(023, 016, 4, 0, 061)).getW(),  //  LR,U    R4,061
            (new InstructionWord(023, 016, 5, 0, 071)).getW(),  //  LR,U    R5,071
            (new InstructionWord(023, 016, 6, 0, 072)).getW(),  //  LR,U    R6,072
            (new InstructionWord(023, 016, 7, 0, 073)).getW(),  //  LR,U    R7,073

            //  put descriptor in A10, then do the transfer into Bank 1, offset 2
            (new InstructionWord(010, 0, 10, 0, 0, 0, 2, 0)).getW(),    //  LA      A10,0,,B2
            (new InstructionWord(072, 016, 10, 0, 0, 0, 1, 2)).getW(),  //  SRS     A10,2,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data1, data2 };

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0, bankData[0]);
        assertEquals(0, bankData[1]);
        assertEquals(021, bankData[2]);
        assertEquals(031, bankData[3]);
        assertEquals(041, bankData[4]);
        assertEquals(051, bankData[5]);
        assertEquals(061, bankData[6]);
        assertEquals(04, bankData[7]);
        assertEquals(05, bankData[8]);
        assertEquals(06, bankData[9]);
    }
}
