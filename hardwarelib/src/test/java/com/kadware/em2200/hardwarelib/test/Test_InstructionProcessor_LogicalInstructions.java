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
public class Test_InstructionProcessor_LogicalInstructions extends Test_InstructionProcessor {


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Instruction Execution, Addressing Modes, and such
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void logicalOR(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_111111_111111l,
            0_222222_222222l,
            0_777000_777000l,
            0_750750_777777l,
            0_777777_777123l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(040, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  OR      A0,1,,B1
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

        assertEquals(0_111111_111111l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_333333l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void logicalXOR(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_111111_111111l,
            0_222222_222222l,
            0_777000_777000l,
            0_750750_777777l,
            0_777777_777123l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 2, 0, 0, 0, 1, 2)).getW(),     //  LA      A2,2,,B1
            (new InstructionWord(041, 2, 2, 0, 0, 0, 1, 3)).getW(),     //  XOR,H1  A2,3,,B1
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

        assertEquals(0_777000_777000l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_777000_027750l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void logicalAND(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_111111_111111l,
            0_222222_222222l,
            0_777000_777000l,
            0_750750_777777l,
            0_777777_777123l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 4, 0, 0, 0, 1, 4)).getW(),     //  LA      A4,4,,B1
            (new InstructionWord(042, 016, 4, 0, 0543321)).getW(),      //  AND,U   A4,0543321
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

        assertEquals(0_777777_777123l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_543121l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void logicalMLU(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777777_000000l,   //  A8
            0_707070_707070l,   //  R2
            0_000000_777777l,   //  op for MLU
        };

        long[] code = {
            (new InstructionWord(010, 0, 010, 0, 0, 0, 1, 0)).getW(),   //  LA      A8,0,,B1
            (new InstructionWord(023, 0, 002, 0, 0, 0, 1, 1)).getW(),   //  LR      R2,1,,B1
            (new InstructionWord(043, 0, 010, 0, 0, 0, 1, 2)).getW(),   //  MLU     A8,2,,B1
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

        assertEquals(0_777777_000000l, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_070707_707070l, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }
}
