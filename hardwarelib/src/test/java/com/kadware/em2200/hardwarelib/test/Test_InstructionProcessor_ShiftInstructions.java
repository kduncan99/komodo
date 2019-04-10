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
public class Test_InstructionProcessor_ShiftInstructions extends Test_InstructionProcessor {

    @Test
    public void singleShiftAlgebraic(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777777_771352l,
        };

        long[] code = {
            (new InstructionWord(010,   0, 05, 0, 0, 0, 1, 0)).getW(),  //  LA      A5,0,,B1
            (new InstructionWord(073, 004, 05, 0, 0, 0, 6)).getW(),     //  SSA     A5,6
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

        assertEquals(0_777777_777713l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void doubleShiftAlgebraic(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_333444_555666l,
            0_777000_111222l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 02, 0, 0, 0, 1, 0)).getW(),    //  LA      A2,0,,B1
            (new InstructionWord(010, 0, 03, 0, 0, 0, 1, 1)).getW(),    //  LA      A3,1,,B1
            (new InstructionWord(073, 005, 02, 0, 0, 0, 12)).getW(),    //  DSA     A2,12
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

        assertEquals(0_000033_344455l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_566677_700011l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void singleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 06, 0, 0, 0, 012345)).getW(),    //  LA,U    A6,012345
            (new InstructionWord(073, 000, 06, 0, 0, 0, 014)).getW(),       //  SSC     A6,014
            (new InstructionWord(010, 016, 00, 0, 0, 0, 017)).getW(),       //  LA,U    A0,017
            (new InstructionWord(073, 000, 06, 014, 0, 0, 003)).getW(),     //  SSC     A6,3,A0
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),      //  IAR     d,x,b
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

        assertEquals(0_000001_234500l, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
    }

    @Test
    public void doubleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000000_000000l,
            0_123456_765432l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 02, 0, 0, 0, 1, 0)).getW(),    //  LA      A2,0,,B1
            (new InstructionWord(010, 0, 03, 0, 0, 0, 1, 1)).getW(),    //  LA      A3,1,,B1
            (new InstructionWord(073, 001, 02, 0, 0, 0, 024)).getW(),   //  DSC     A2,024
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

        assertEquals(0_575306_400000l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_024713l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void singleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000123_366157l,
        };

        long[] code = {
            (new InstructionWord(010, 000, 05, 0, 0, 0, 1, 0)).getW(),  //  LA      A5,0,,B1
            (new InstructionWord(073, 002, 05, 0, 0, 0, 02)).getW(),    //  SSL     A5,2
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

        assertEquals(0_000024_675433l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void doubleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777666_555444l,
            0_333222_112345l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 02, 0, 0, 0, 1, 0)).getW(),    //  LA      A2,,0,B1
            (new InstructionWord(010, 0, 03, 0, 0, 0, 1, 1)).getW(),    //  LA      A3,,1,B1
            (new InstructionWord(073, 003, 02, 0, 0, 0, 011)).getW(),   //  DSL     A2,9
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

        assertEquals(0_000777_666555l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_444333_222112l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void loadShiftAndCount(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_400000_000000l,   //  normalized
            0_377777_777777l,   //  normalized
            0_000000_000000l,   //  zero
            0_777777_777777l,   //  also zero
            0_001111_111111l,   //  7 shifts required
        };

        long[] code = {
            (new InstructionWord(073, 06,  00, 0, 0, 0, 1, 0)).getW(),      //  LSC     A0,0,,B1
            (new InstructionWord(073, 06,  02, 0, 0, 0, 1, 1)).getW(),      //  LSC     A2,1,,B1
            (new InstructionWord(073, 06,  04, 0, 0, 0, 1, 2)).getW(),      //  LSC     A4,2,,B1
            (new InstructionWord(073, 06,  06, 0, 0, 0, 1, 3)).getW(),      //  LSC     A6,2,,B1
            (new InstructionWord(073, 06, 010, 0, 0, 0, 1, 4)).getW(),      //  LSC     A8,2,,B1
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

        assertEquals(0_400000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_377777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_000000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(35, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(35, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(0_222222_222200l, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(7, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void doubleLoadShiftAndCount(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_400000_000000l,
            0_000000_000000l,   //  normalized
            0_377777_777777l,
            0_777777_777777l,   //  normalized
            0_000000_000000l,
            0_000000_000000l,   //  zero
            0_777777_777777l,
            0_777777_777777l,   //  also zero
            0_000000_000000l,
            0_333444_555666l,   //  36 shifts required
        };

        long[] code = {
            (new InstructionWord(073, 07,  00, 0, 0, 0, 1, 0)).getW(),      //  DLSC    A0,0,,B1
            (new InstructionWord(073, 07,  03, 0, 0, 0, 1, 2)).getW(),      //  DLSC    A3,2,,B1
            (new InstructionWord(073, 07,  06, 0, 0, 0, 1, 4)).getW(),      //  DLSC    A6,4,,B1
            (new InstructionWord(073, 07, 011, 0, 0, 0, 1, 6)).getW(),      //  DLSC    A9,6,,B1
            (new InstructionWord(073, 07, 014, 0, 0, 0, 1, 8)).getW(),      //  DLSC    A12,8,,B1
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

        assertEquals(0_400000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_377777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertEquals(0_000000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        assertEquals(0_000000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
        assertEquals(71, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A10).getW());
        assertEquals(71, ip.getGeneralRegister(GeneralRegisterSet.A11).getW());
        assertEquals(0_333444_555666l, ip.getGeneralRegister(GeneralRegisterSet.A12).getW());
        assertEquals(0_000000_000000l, ip.getGeneralRegister(GeneralRegisterSet.A13).getW());
        assertEquals(36, ip.getGeneralRegister(GeneralRegisterSet.A14).getW());
    }

    @Test
    public void leftSingleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_776655_443322l,
        };

        long[] code = {
            (new InstructionWord(010, 000, 05, 0, 0, 0, 1, 0)).getW(),  //  LA      A5,0,,B1
            (new InstructionWord(073, 010, 05, 0, 0, 0, 15)).getW(),    //  LSSL    A5,15
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

        assertEquals(0_544332_277665l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void leftDoubleShiftCircular(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777777_600000l,
            0_666666_666666l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 02, 0, 0, 0, 1, 0)).getW(),    //  LA      A2,0,,B1
            (new InstructionWord(010, 0, 03, 0, 0, 0, 1, 1)).getW(),    //  LA      A3,1,,B1
            (new InstructionWord(073, 011, 02, 0, 0, 0, 024)).getW(),   //  LDSC    A2,20
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

        assertEquals(0_000003_333333l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_333333_777777l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void leftSingleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000123_366157l,
        };

        long[] code = {
            (new InstructionWord(010, 000, 05, 0, 0, 0, 1, 0)).getW(),  //  LA      A5,0,,B1
            (new InstructionWord(073, 012, 05, 0, 0, 0, 03)).getW(),    //  LSSL    A5,3
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

        assertEquals(0_001233_661570l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void leftDoubleShiftLogical(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_777666_555444l,
            0_333222_112345l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 02, 0, 0, 0, 1, 0)).getW(),    //  LA      A2,,0,B1
            (new InstructionWord(010, 0, 03, 0, 0, 0, 1, 1)).getW(),    //  LA      A3,,1,B1
            (new InstructionWord(073, 013, 02, 0, 0, 0, 011)).getW(),   //  LDSL    A2,9
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

        assertEquals(0_666555_444333l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_222112_345000l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }
}
