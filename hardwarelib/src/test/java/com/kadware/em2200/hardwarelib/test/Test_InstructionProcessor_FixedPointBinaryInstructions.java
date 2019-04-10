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
public class Test_InstructionProcessor_FixedPointBinaryInstructions extends Test_InstructionProcessor {


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Instruction Execution, Addressing Modes, and such
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void addAccumulator(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 007)).getW(),          //  LA,U    A0,007
            (new InstructionWord(014, 016, 0, 0, 014)).getW(),          //  AA.U    A0,014
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0)).getW(),            //  LA,U    A0,0
            (new InstructionWord(014, 016, 0, 0, 0)).getW(),            //  AA.U    A0,0
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(005, 0, 01, 0, 0, 0, GeneralRegisterSet.A5)).getW(),   //  SNZ     A5
            (new InstructionWord(010, 0, 00, 0, 0, 0, GeneralRegisterSet.A5)).getW(),   //  LA      A0,A5
            (new InstructionWord(014, 0, 00, 0, 0, 0, GeneralRegisterSet.A5)).getW(),   //  AA      A0,A5
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),                  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertTrue(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeAccumulator(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0234,
            0236
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(015, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  ANA     A0,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777775l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addMagnitudeAccumulator_positive(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0234,
            0236
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(016, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  AMA     A0,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0234,
            0777777_777775l
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(016, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  AMA     A0,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0234,
            0236
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,0,,B1
            (new InstructionWord(017, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  ANMA    A0,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0777777_777775l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulatorUpper(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 007)).getW(),          //  LA,U    A0,007
            (new InstructionWord(020, 016, 0, 0, 014)).getW(),          //  AU.U    A0,014
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 007)).getW(),          //  LA,U    A0,007
            (new InstructionWord(021, 016, 0, 0, 014)).getW(),          //  ANU.U   A0,014
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(07, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777772l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addIndexRegister(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(027, 016, 0, 0, 007)).getW(),          //  LX,U    X0,007
            (new InstructionWord(024, 016, 0, 0, 014)).getW(),          //  AX.U    X0,014
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0234,
            0236
        };

        long[] code = {
            (new InstructionWord(027, 016, 0, 0, 007)).getW(),          //  LX,U    X0,007
            (new InstructionWord(025, 016, 0, 0, 014)).getW(),          //  ANX.U   X0,014
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_777777_777772l, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addAccumulator_Overflow(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            OnesComplement.LARGEST_POSITIVE_INTEGER_36,
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data
            (new InstructionWord(014, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  AA      A0,data
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
        dReg.setOperationTrapEnabled(true);

        long[][] interruptCode = new long[64][];
        setupInterrupts(ip, msp, 02000, interruptCode);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        //???? look for evidence of operation trap interrupt
    }

    @Test
    public void addHalves(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000123_555123l,
            0_000001_223000l,
        };

        long[] code = {
            (new InstructionWord(010,  0, 05, 0, 0, 0, 1, 0)).getW(),   //  LA      A5,0,,B1
            (new InstructionWord(072, 04, 05, 0, 0, 0, 1, 1)).getW(),   //  AH      A5,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000123_555123l,
            0_777776_332123l,
        };

        long[] code = {
            (new InstructionWord(010,  0, 03, 0, 0, 0, 1, 0)).getW(),   //  LA      A3,0,,B1
            (new InstructionWord(072, 05, 03, 0, 0, 0, 1, 1)).getW(),   //  ANH     A3,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000124_223000l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addThirds(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_000123_555123l,
            0_000001_223000l,
        };

        long[] code = {
            (new InstructionWord(010,  0, 05, 0, 0, 0, 1, 0)).getW(),   //  LA      A5,0,,B1
            (new InstructionWord(072, 06, 05, 0, 0, 0, 1, 1)).getW(),   //  AT      A5,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000124_770124l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void addNegativeThirds(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_0001_0122_5123l,
            0_0000_2355_3000l,
        };

        long[] code = {
            (new InstructionWord(010,  0, 03, 0, 0, 0, 1, 0)).getW(),   //  LA      A3,0,,B1
            (new InstructionWord(072, 07, 03, 0, 0, 0, 1, 1)).getW(),   //  ANT     A3,1,,B1
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_0001_5544_2123l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void divideInteger(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // Example from the hardware guide
        long[] data = {
            0_000000_011416l,
            0_110621_672145l,
            0_000001_635035l
        };

        long[] code = {
            (new InstructionWord(010, 0, 2, 0, 0, 0, 1, 0)).getW(),     //  LA      A2,data[0]
            (new InstructionWord(010, 0, 3, 0, 0, 0, 1, 1)).getW(),     //  LA      A3,data[1]
            (new InstructionWord(034, 0, 2, 0, 0, 0, 1, 2)).getW(),     //  DI      A2,data[2]
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
        dReg.setArithmeticExceptionEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_005213_747442l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000000_244613l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void divideInteger_byZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void divideInteger_byZero_noInterrupt(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        long[] data = {
            0_111111_222222l,
            0_333333_444444l,
            0l
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data[0]
            (new InstructionWord(010, 0, 1, 0, 0, 0, 1, 1)).getW(),     //  LA      A1,data[1]
            (new InstructionWord(034, 0, 0, 0, 0, 0, 1, 2)).getW(),     //  DI      A0,data[2]
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
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    @Test
    public void divideInteger_byNegativeZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void divideSingleFractional(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // Example from the hardware guide
        long[] data = {
            0_000000_007236l,
            0_743464_241454l,
            0_000001_711467l
        };

        long[] code = {
            (new InstructionWord(010, 0, 3, 0, 0, 0, 1, 0)).getW(),     //  LA      A3,data[0]
            (new InstructionWord(010, 0, 4, 0, 0, 0, 1, 1)).getW(),     //  LA      A4,data[1]
            (new InstructionWord(035, 0, 3, 0, 0, 0, 1, 2)).getW(),     //  DSF     A3,data[2]
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
        dReg.setArithmeticExceptionEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_001733_765274l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    @Test
    public void divideSingleFractional_byZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void divideSingleFractional_byZero_noInterrupt(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        long[] data = {
            0_111111_222222l,
            0_333333_444444l,
            0l
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data[0]
            (new InstructionWord(010, 0, 1, 0, 0, 0, 1, 1)).getW(),     //  LA      A1,data[1]
            (new InstructionWord(035, 0, 0, 0, 0, 0, 1, 2)).getW(),     //  DSF     A0,data[2]
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
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    @Test
    public void divideSingleFractional_byNegativeZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void divideFractional(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // Example from the hardware guide
        long[] data = {
            0,
            0_000061_026335l,
            0_000000_001300l
        };

        long[] code = {
            (new InstructionWord(010, 0, 4, 0, 0, 0, 1, 0)).getW(),     //  LA      A4,data[0]
            (new InstructionWord(010, 0, 5, 0, 0, 0, 1, 1)).getW(),     //  LA      A5,data[1]
            (new InstructionWord(036, 0, 4, 0, 0, 0, 1, 2)).getW(),     //  DF      A4,data[2]
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
        dReg.setArithmeticExceptionEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_000000_021653l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        assertEquals(0_000000_000056l, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void divideFractional_byZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void divideFractional_byZero_noInterrupt(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        long[] data = {
            0_111111_222222l,
            0_333333_444444l,
            0l
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data[0]
            (new InstructionWord(010, 0, 1, 0, 0, 0, 1, 1)).getW(),     //  LA      A1,data[1]
            (new InstructionWord(036, 0, 0, 0, 0, 0, 1, 2)).getW(),     //  DF      A0,data[2]
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
        dReg.setArithmeticExceptionEnabled(false);
        dReg.setDivideCheck(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(dReg.getDivideCheck());
    }

    @Test
    public void divideFractional_byNegativeZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             //???? implement when we can catch arithmetic exception interrupt
    }

    @Test
    public void doubleAdd(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_111111_222222l,
            0_333333_444444l,
            0_222222_333333l,
            0_000000_111111l,
        };

        long[] code = {
            (new InstructionWord(071, 013, 0, 0, 0, 0, 1, 0)).getW(),   //  DL      A0,data[0]
            (new InstructionWord(071, 010, 0, 0, 0, 0, 1, 2)).getW(),   //  DA      A0,data[2]
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_333333_555555l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_555555l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertFalse(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void doubleAddNegative(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_333333_222222l,
            0_777777_666666l,
            0_111111_222222l,
            0_444444_333333l,
        };

        long[] code = {
            (new InstructionWord(071, 013, 0, 0, 0, 0, 1, 0)).getW(),   //  DL      A0,data[0]
            (new InstructionWord(071, 011, 0, 0, 0, 0, 1, 2)).getW(),   //  DAN     A0,data[2]
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
        };

        long[][] sourceData = { code, data };

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        loadBanks(ip, msp, 0, sourceData);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(0);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0_222222_000000l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_333333_333333l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertTrue(ip.getDesignatorRegister().getCarry());
        assertFalse(ip.getDesignatorRegister().getOverflow());
    }

    @Test
    public void multiplyInteger(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_377777_777777l,   //  data
            0_777777_777776l,
            0_002244_113355l,
        };

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data[0]
            (new InstructionWord(030, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  MI      A0,data[0]
            (new InstructionWord(010, 0, 2, 0, 0, 0, 1, 1)).getW(),     //  LA      A0,data[1]
            (new InstructionWord(030, 0, 2, 0, 0, 0, 1, 2)).getW(),     //  MI      A0,data[2]
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

        assertEquals(0_177777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000000_000001l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_777777_777777l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_775533_664422l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void multiplySingleInteger(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = { 200, 520 }; //  data

        long[] code = {
            (new InstructionWord(010, 0, 0, 0, 0, 0, 1, 0)).getW(),     //  LA      A0,data[0]
            (new InstructionWord(031, 0, 0, 0, 0, 0, 1, 1)).getW(),     //  MSI     A0,data[1]
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

        assertEquals(200 * 520, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    //???? Need an MSI overflow test once we can handle interrupts

    @Test
    public void multiplyFractional(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_200000_000002l, 0_777777_777777l, 0_111111_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 0, 3, 0, 0, 0, 1, 0)).getW(),     //  LA      A3,data[0]
            (new InstructionWord(010, 0, 4, 0, 0, 0, 1, 1)).getW(),     //  LA      A4,data[1]
            (new InstructionWord(032, 0, 3, 0, 0, 0, 1, 2)).getW(),     //  MF      A3,data[2]
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

        assertEquals(0_044444_444445l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_044444_444444l, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
    }

    //???? need add1 test for basic mode pp>0 (throws machine interrupt)

    @Test
    public void add1(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_777776_111111l,
                        0_0000_7777_0000l,
                        0_777777_777776l }; //  data

        long[] code = {
            (new InstructionWord(005, 02, 015, 0, 0, 0, 1, 0)).getW(),  //  ADD1,H1 data[0] (H1 2's comp)
            (new InstructionWord(005, 06, 015, 0, 0, 0, 1, 1)).getW(),  //  ADD1,T2 data[1] (T2 1's comp)
            (new InstructionWord(005, 00, 015, 0, 0, 0, 1, 2)).getW(),  //  ADD1    data[2] (W 1's comp)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_777777_111111l, bankData[0]);
        assertEquals(0_0000_0001_0000l, bankData[1]);
        assertEquals(0_000000_000000l, bankData[2]);

        //  check overflow and carry from the last instruction
        assertTrue(dReg.getCarry());
        assertFalse(dReg.getOverflow());
    }

    //???? need sub1 test for basic mode pp>0 (throws machine interrupt)

    @Test
    public void sub1(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_5555_0001_5555l,
                        0_000000_000000l,
                        0_000000_000000l }; //  data

        long[] code = {
            (new InstructionWord(005, 06, 016, 0, 0, 0, 1, 0)).getW(),  //  SUB1,T2 data[0] (H1 1's comp)
            (new InstructionWord(005, 02, 016, 0, 0, 0, 1, 1)).getW(),  //  SUB1,H1 data[1] (T2 2's comp)
            (new InstructionWord(005, 00, 016, 0, 0, 0, 1, 2)).getW(),  //  SUB1    data[2] (W 1's comp)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_5555_0000_5555l, bankData[0]);
        assertEquals(0_777777_000000l, bankData[1]);
        assertEquals(0_777777_777776l, bankData[2]);

        //  check overflow and carry from the last instruction
        assertFalse(dReg.getCarry());
        assertFalse(dReg.getOverflow());
    }

    @Test
    public void inc(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_000000_000000l,
                        0_777777_777776l,
                        0_000010_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 016,   0, 0, 0, 0, 0)).getW(),    //  LA,U    A0,0
            (new InstructionWord(010, 016,  01, 0, 0, 0, 0)).getW(),    //  LA,U    A1,0
            (new InstructionWord(010, 016,  02, 0, 0, 0, 0)).getW(),    //  LA,U    A2,0
            (new InstructionWord(005,   0, 010, 0, 0, 0, 1, 0)).getW(), //  INC     data[0] (W 1's comp)
            (new InstructionWord(010, 016,   0, 0, 0, 0, 1)).getW(),    //  LA,U    A0,1 (should be skipped)
            (new InstructionWord(005,   0, 010, 0, 0, 0, 1, 1)).getW(), //  INC     data[1] (W 1's comp)
            (new InstructionWord(010, 016,  01, 0, 0, 0, 1)).getW(),    //  LA,U    A1,1 (should be skipped)
            (new InstructionWord(005,  02, 010, 0, 0, 0, 1, 2)).getW(), //  INC,H1  data[2] (H1 2's comp)
            (new InstructionWord(010, 016,  02, 0, 0, 0, 1)).getW(),    //  LA,U    A2,1 (should be executed)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000001l, bankData[0]);
        assertEquals(0_000000_000000l, bankData[1]);
        assertEquals(0_000011_111111l, bankData[2]);
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_000000_000001l,
                        0_777777_777777l,
                        0_000010_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 016,   0, 0, 0, 0, 0)).getW(),    //  LA,U    A0,0
            (new InstructionWord(010, 016,  01, 0, 0, 0, 0)).getW(),    //  LA,U    A1,0
            (new InstructionWord(010, 016,  02, 0, 0, 0, 0)).getW(),    //  LA,U    A2,0
            (new InstructionWord(005,   0, 011, 0, 0, 0, 1, 0)).getW(), //  DEC     data[0] (W 1's comp)
            (new InstructionWord(010, 016,   0, 0, 0, 0, 1)).getW(),    //  LA,U    A0,1 (should be skipped)
            (new InstructionWord(005,   0, 011, 0, 0, 0, 1, 1)).getW(), //  DEC     data[1] (W 1's comp)
            (new InstructionWord(010, 016,  01, 0, 0, 0, 1)).getW(),    //  LA,U    A1,1 (should be skipped)
            (new InstructionWord(005,  02, 011, 0, 0, 0, 1, 2)).getW(), //  DEC,H1  data[2] (H1 2's comp)
            (new InstructionWord(010, 016,  02, 0, 0, 0, 1)).getW(),    //  LA,U    A2,1 (should be executed)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000000l, bankData[0]);
        assertEquals(0_777777_777776l, bankData[1]);
        assertEquals(0_000007_111111l, bankData[2]);
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void inc2(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_000000_000000l,
                        0_777777_777775l,
                        0_000010_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 016,   0, 0, 0, 0, 0)).getW(),    //  LA,U    A0,0
            (new InstructionWord(010, 016,  01, 0, 0, 0, 0)).getW(),    //  LA,U    A1,0
            (new InstructionWord(010, 016,  02, 0, 0, 0, 0)).getW(),    //  LA,U    A2,0
            (new InstructionWord(005,   0, 012, 0, 0, 0, 1, 0)).getW(), //  INC2    data[0] (W 1's comp)
            (new InstructionWord(010, 016,   0, 0, 0, 0, 1)).getW(),    //  LA,U    A0,1 (should be skipped)
            (new InstructionWord(005,   0, 012, 0, 0, 0, 1, 1)).getW(), //  INC2    data[1] (W 1's comp)
            (new InstructionWord(010, 016,  01, 0, 0, 0, 1)).getW(),    //  LA,U    A1,1 (should be skipped)
            (new InstructionWord(005,  02, 012, 0, 0, 0, 1, 2)).getW(), //  INC2,H1 data[2] (H1 2's comp)
            (new InstructionWord(010, 016,  02, 0, 0, 0, 1)).getW(),    //  LA,U    A2,1 (should be executed)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000002l, bankData[0]);
        assertEquals(0_000000_000000l, bankData[1]);
        assertEquals(0_000012_111111l, bankData[2]);
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void dec2(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_000000_000002l,
                        0_000000_000000l,
                        0_000010_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 016,   0, 0, 0, 0, 0)).getW(),    //  LA,U    A0,0
            (new InstructionWord(010, 016,  01, 0, 0, 0, 0)).getW(),    //  LA,U    A1,0
            (new InstructionWord(010, 016,  02, 0, 0, 0, 0)).getW(),    //  LA,U    A2,0
            (new InstructionWord(005,   0, 013, 0, 0, 0, 1, 0)).getW(), //  DEC2    data[0] (W 1's comp)
            (new InstructionWord(010, 016,   0, 0, 0, 0, 1)).getW(),    //  LA,U    A0,1 (should be skipped)
            (new InstructionWord(005,   0, 013, 0, 0, 0, 1, 1)).getW(), //  DEC2    data[1] (W 1's comp)
            (new InstructionWord(010, 016,  01, 0, 0, 0, 1)).getW(),    //  LA,U    A1,1 (should be skipped)
            (new InstructionWord(005,  02, 013, 0, 0, 0, 1, 2)).getW(), //  DEC2,H1 data[2] (H1 2's comp)
            (new InstructionWord(010, 016,  02, 0, 0, 0, 1)).getW(),    //  LA,U    A2,1 (should be executed)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000000l, bankData[0]);
        assertEquals(0_777777_777775l, bankData[1]);
        assertEquals(0_000006_111111l, bankData[2]);
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void ienz(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        long[] data = { 0_000000_000000l,
                        0_777777_777777l,
                        0_000010_111111l }; //  data

        long[] code = {
            (new InstructionWord(010, 016,   0, 0, 0, 0, 0)).getW(),    //  LA,U    A0,0
            (new InstructionWord(010, 016,  01, 0, 0, 0, 0)).getW(),    //  LA,U    A1,0
            (new InstructionWord(010, 016,  02, 0, 0, 0, 0)).getW(),    //  LA,U    A2,0
            (new InstructionWord(005,   0, 014, 0, 0, 0, 1, 0)).getW(), //  ENZ     data[0] (W 1's comp)
            (new InstructionWord(010, 016,   0, 0, 0, 0, 1)).getW(),    //  LA,U    A0,1 (should be skipped)
            (new InstructionWord(005,   0, 014, 0, 0, 0, 1, 1)).getW(), //  ENZ     data[1] (W 1's comp)
            (new InstructionWord(010, 016,  01, 0, 0, 0, 1)).getW(),    //  LA,U    A1,1 (should be skipped)
            (new InstructionWord(005,  02, 014, 0, 0, 0, 1, 2)).getW(), //  ENZ,H1  data[2] (H1 2's comp)
            (new InstructionWord(010, 016,  02, 0, 0, 0, 1)).getW(),    //  LA,U    A2,1 (should be executed)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  IAR     d,x,b
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

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000000l, bankData[0]);
        assertEquals(0_000000_000000l, bankData[1]);
        assertEquals(0_000010_111111l, bankData[2]);
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }
}
