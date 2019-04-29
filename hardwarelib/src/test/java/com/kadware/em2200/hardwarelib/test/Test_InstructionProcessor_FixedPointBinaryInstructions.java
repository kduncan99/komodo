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

//        long[][] interruptCode = new long[64][];
//        setupInterrupts(ip, msp, 02000, interruptCode);

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
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 000123555123",
                "DATA2     + 000001223000",
                "",
                "$(1),START*",
                "          LA        A5,DATA1,,B1",
                "          AT        A5,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 00001,00122,05123",
                "DATA2     + 00000,02355,03000",
                "",
                "$(1),START*",
                "          LA        A3,DATA1,,B1",
                "          ANT       A3,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
             // Example from the hardware guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 011416",
                "          + 0110621,0672145",
                "DATA2     + 01,0635035",
                "$(1),START*",
                "          DL        A2,DATA1,,B1",
                "          DI        A2,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 0111111,0222222",
                "          + 0333333,0444444",
                "$(1),START*",
                "          DL        A0,DATA1,,B1",
                "          DI,U      A0,0",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // Example from the hardware guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 07236",
                "          + 0743464241454",
                "DATA2     + 01711467",
                "$(1),START*",
                "          DL        A3,DATA1,,B1",
                "          DSF       A3,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 0111111222222",
                "          + 0333333444444",
                "DATA2     + 0",
                "$(1),START*",
                "          DL        A0,DATA1,,B1",
                "          DSF       A0,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // Example from the hardware guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 0",
                "          + 061026335",
                "DATA2     + 01300",
                "$(1),START*",
                "          DL        A4,DATA1,,B1",
                "          DF        A4,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        // disable arithmetic exception interrupt, and look for zeros in the resulting registers
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "DATA1     + 0111111222222",
                "          + 0333333444444",
                "DATA2     + 0",
                "$(1),START*",
                "          DL        A0,DATA1,,B1",
                "          DF        A0,DATA2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $BASIC",
                "          $LIT 0",
                "$(0)",
                "ADDEND1   + 0111111222222",
                "          + 0333333444444",
                "ADDEND2   + 0222222333333",
                "          + 0000000111111",
                "$(1),START*",
                "          DL        A0,ADDEND1",
                "          DA        A0,ADDEND2",
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $BASIC",
                "          $LIT 0",
                "$(0)",
                "ADDEND1   + 0333333222222",
                "          + 0777777666666",
                "ADDEND2   + 0111111222222",
                "          + 0444444333333",
                "$(1),START*",
                "          DL        A0,ADDEND1",
                "          DAN       A0,ADDEND2",
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(1),START*",
                "          LA        A0,(0377777777777),,B1",
                "          MI        A0,(0377777777777),,B1",
                "          LA        A2,(0777777777776),,B1",
                "          MI        A2,(0002244113355),,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(1),START*",
                "          LA,U      A0,200",
                "          MSI       A0,(520),,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(0)",
                "FACTOR1   0200000000002",
                "          0777777777777",
                "FACTOR2   0111111111111",
                "",
                "$(1),START*",
                "          LA        A3,FACTOR1,,B1",
                "          LA        A4,FACTOR1+1,,B1",
                "          MF        A3,FACTOR2,,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

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

    //TODO need add1 test for basic mode pp>0 (throws machine interrupt)

    @Test
    public void add1(
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(1),START*",
                "          ADD1,H1   (0777776,0111111),,B1",
                "          ADD1,T2   (0,07777,0),,B1",
                "          ADD1      (0777777777776),,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
        assertEquals(0_777777_111111L, bankData[0]);
        assertEquals(0_0000_0001_0000L, bankData[1]);
        assertEquals(0_000000_000000L, bankData[2]);

        //  check overflow and carry from the last instruction
        assertTrue(dReg.getCarry());
        assertFalse(dReg.getOverflow());
    }

    //TODO need sub1 test for basic mode pp>0 (throws machine interrupt)

    @Test
    public void sub1(
    ) throws MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(1),START*",
                "          SUB1,T2   (05555,0001,05555),,B1",
                "          SUB1,H1   (0),,B1",
                "          SUB1      (0),,B1",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(false);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
        assertEquals(0_5555_0000_5555L, bankData[0]);
        assertEquals(0_777777_000000L, bankData[1]);
        assertEquals(0_777777_777776L, bankData[2]);

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
        String[] source = {
                "          $EXTEND",
                "          $LIT 0",
                "$(1),START*",
                "          LA,U      A0,0",
                "          LA,U      A1,0",
                "          LA,U      A2,0",
                "          INC       (0),,B1",
                "          LA,U      A0,1                . should be skipped",
                "          INC       (0777777777776),,B1",
                "          LA,U      A1,1                . should be skipped",
                "          INC,H1    (010111111),,B1",
                "          LA,U      A2,1                . should be executed",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "",
                "          $LIT 0",
                "",
                "$(1),START*",
                "          LA,U      A0,0",
                "          LA,U      A1,0",
                "          LA,U      A2,0",
                "          DEC       (01),,B1",
                "          LA,U      A0,1                . should be skipped",
                "          DEC       (0777777777777),,B1",
                "          LA,U      A1,1                . should be skipped",
                "          DEC,H1    (010111111),,B1",
                "          LA,U      A2,1                . should be executed",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "",
                "          $LIT 0",
                "",
                "$(1),START*",
                "          LA,U      A0,0",
                "          LA,U      A1,0",
                "          LA,U      A2,0",
                "          INC2      (0),,B1",
                "          LA,U      A0,1                . should be skipped",
                "          INC2      (0777777777775),,B1",
                "          LA,U      A1,1                . should be skipped",
                "          INC2,H1   (010111111),,B1",
                "          LA,U      A2,1                . should be executed",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "",
                "          $LIT 0",
                "",
                "$(1),START*",
                "          LA,U      A0,0",
                "          LA,U      A1,0",
                "          LA,U      A2,0",
                "          DEC2      (02),,B1",
                "          LA,U      A0,1                . should be skipped",
                "          DEC2      (0),,B1",
                "          LA,U      A1,1                . should be skipped",
                "          DEC2,H1   (010,0111111),,B1",
                "          LA,U      A2,1                . should be executed",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
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
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //  This test is per the hardware instruction guide
        String[] source = {
                "          $EXTEND",
                "",
                "          $LIT 0",
                "",
                "$(1),START*",
                "          LA,U      A0,0",
                "          LA,U      A1,0",
                "          LA,U      A2,0",
                "          ENZ       (0),,B1",
                "          LA,U      A0,1                . should be skipped",
                "          ENZ       (0777777,0777777),,B1",
                "          LA,U      A1,1                . should be skipped",
                "          ENZ,H1    (010,0111111),,B1",
                "          LA,U      A2,1                . should be executed",
                "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);

        TestProcessor ip = new TestProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        loadBanks(ip, msp, absoluteModule);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(false);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        long[] bankData = getBank(ip, 1);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_000000_000000L, bankData[1]);
        assertEquals(0_000010_111111L, bankData[2]);
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_0L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_1L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }
}
