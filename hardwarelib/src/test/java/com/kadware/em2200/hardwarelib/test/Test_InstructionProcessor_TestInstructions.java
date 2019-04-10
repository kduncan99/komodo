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
public class Test_InstructionProcessor_TestInstructions extends Test_InstructionProcessor {

    @Test
    public void testEvenOddParity(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777777_771356l,
            0_000000_007777l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(010,   0, 1, 0, 0, 0, 1, 0)).getW(),   //  LA      A1,0,,B1
            (new InstructionWord(044, 005, 1, 0, 0, 0, 1, 1)).getW(),   //  TEP,Q4  A1,1,,B1
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A0,1        . this should be skipped

            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A2,0
            (new InstructionWord(010,   0, 1, 0, 0, 0, 1, 0)).getW(),   //  LA      A1,0,,B1
            (new InstructionWord(045, 001, 1, 0, 0, 0, 1, 1)).getW(),   //  TOP,H2  A1,1,,B1
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A2,1        . this should be skipped

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void testZeroNonZeroPosNegZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_0l,
            0_777777_777777l,
            0_1l
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(010, 016, 1, 0, 0)).getW(),            //  LA,U    A1,0
            (new InstructionWord(010, 016, 2, 0, 0)).getW(),            //  LA,U    A2,0
            (new InstructionWord(010, 016, 3, 0, 0)).getW(),            //  LA,U    A3,0
            (new InstructionWord(010, 016, 4, 0, 0)).getW(),            //  LA,U    A4,0
            (new InstructionWord(010, 016, 5, 0, 0)).getW(),            //  LA,U    A5,0
            (new InstructionWord(010, 016, 6, 0, 0)).getW(),            //  LA,U    A6,0
            (new InstructionWord(010, 016, 7, 0, 0)).getW(),            //  LA,U    A7,0

            (new InstructionWord(050, 00, 06, 0, 0, 0, 1, 0)).getW(),   //  TZ      0,,B1
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A0,1    . this should be skipped

            (new InstructionWord(050, 00, 06, 0, 0, 0, 1, 1)).getW(),   //  TZ      1,,B1
            (new InstructionWord(010, 016, 1, 0, 1)).getW(),            //  LA,U    A1,1    . this should be skipped

            (new InstructionWord(050, 00, 06, 0, 0, 0, 1, 2)).getW(),   //  TZ      2,,B1
            (new InstructionWord(010, 016, 2, 0, 1)).getW(),            //  LA,U    A2,1    . should not be skipped

            (new InstructionWord(050, 00, 011, 0, 0, 0, 1, 0)).getW(),  //  TNZ     0,,B1
            (new InstructionWord(010, 016, 3, 0, 1)).getW(),            //  LA,U    A3,1    . should not be skipped

            (new InstructionWord(050, 00, 011, 0, 0, 0, 1, 1)).getW(),  //  TNZ     1,,B1
            (new InstructionWord(010, 016, 4, 0, 1)).getW(),            //  LA,U    A4,1    . should not be skipped

            (new InstructionWord(050, 00, 011, 0, 0, 0, 1, 2)).getW(),  //  TNZ     2,,B1
            (new InstructionWord(010, 016, 5, 0, 1)).getW(),            //  LA,U    A5,1    . this should be skipped

            (new InstructionWord(050, 00, 02, 0, 0, 0, 1, 0)).getW(),   //  TPZ     0,,B1
            (new InstructionWord(010, 016, 6, 0, 1)).getW(),            //  LA,U    A6,1    . this should be skipped

            (new InstructionWord(050, 00, 04, 0, 0, 0, 1, 0)).getW(),   //  TMZ     0,,B1
            (new InstructionWord(010, 016, 7, 0, 1)).getW(),            //  LA,U    A7,1    . this should be skipped

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void testPosNeg(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_0l,
            0_777777_777777l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(010, 016, 1, 0, 0)).getW(),            //  LA,U    A1,0

            (new InstructionWord(050, 00, 03, 0, 0, 0, 1, 0)).getW(),   //  TP      0,,B1
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A0,1    . this should be skipped

            (new InstructionWord(050, 00, 014, 0, 0, 0, 1, 1)).getW(),  //  TN      1,,B1
            (new InstructionWord(010, 016, 1, 0, 1)).getW(),            //  LA,U    A1,1    . this should be skipped

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void testNOPAndTestSkip(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_0l,
            0_0l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(010, 016, 1, 0, 0)).getW(),            //  LA,U    A1,0
            (new InstructionWord(026, 016, 02, 0, 0, 0, 0)).getW(),     //  LXM,U   X2,0
            (new InstructionWord(046, 016, 02, 0, 0, 0, 1)).getW(),     //  LXI,U   X2,1

            (new InstructionWord(050, 00,  0, 2, 1, 0, 1, 0)).getW(),   //  TNOP    0,*X2,B1
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A0,1    . this should not be skipped

            (new InstructionWord(050, 00, 017, 2, 1, 0, 1, 0)).getW(),  //  TSKP    0,*X2,B1
            (new InstructionWord(010, 016, 1, 0, 1)).getW(),            //  LA,U    A1,1    . this should be skipped

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

        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_000001_000002l, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());
    }

    @Test
    public void testEqualNotEqual(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_0l,
            0_0777777_777777l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(010, 016, 1, 0, 0)).getW(),            //  LA,U    A1,0
            (new InstructionWord(010, 016, 2, 0, 0)).getW(),            //  LA,U    A2,0
            (new InstructionWord(010, 016, 3, 0, 0)).getW(),            //  LA,U    A3,0
            (new InstructionWord(010, 016, 4, 0, 0)).getW(),            //  LA,U    A4,0
            (new InstructionWord(010, 016, 10, 0, 0)).getW(),           //  LA,U    A10,0
            (new InstructionWord(010, 0, 11, 0, 0, 0, 1, 1)).getW(),    //  LA      A11,1,,1

            (new InstructionWord(052, 0, 10, 0, 0, 0, 1, 0)).getW(),    //  TE      A10,0,,1    . should skip
            (new InstructionWord(010, 016, 0, 0, 1)).getW(),            //  LA,U    A0,1

            (new InstructionWord(052, 0, 10, 0, 0, 0, 1, 1)).getW(),    //  TE      A10,1,,1    . should not skip
            (new InstructionWord(010, 016, 1, 0, 1)).getW(),            //  LA,U    A1,1

            (new InstructionWord(052, 0, 11, 0, 0, 0, 1, 0)).getW(),    //  TE      A11,0,,1    . should not skip
            (new InstructionWord(010, 016, 2, 0, 1)).getW(),            //  LA,U    A2,1

            (new InstructionWord(052, 0, 11, 0, 0, 0, 1, 1)).getW(),    //  TE      A11,1,,1    . should skip
            (new InstructionWord(010, 016, 3, 0, 1)).getW(),            //  LA,U    A3,1

            (new InstructionWord(071, 017, 10, 0, 0, 0, 1, 0)).getW(),  //  DTE     A10,0,,1    . should skip
            (new InstructionWord(010, 016, 4, 0, 1)).getW(),            //  LA,U    A4,1

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    //  TLEM
    //  TGZ
    //  TMZG
    //  TNLZ
    //  TLZ
    //  TPZL
    //  TNMZ
    //  TNPZ
    //  TNGZ
    //  TLE
    //  TG
    //  TGM
    //  TW
    //  TNW

    //  DTGM

    //  MTE
    //  MTNE
    //  MTLE
    //  MTG
    //  MTW
    //  MTNW
    //  MATL
    //  MATG

    //  TS
    //  TSS
    //  TCS
    //  CR
}
