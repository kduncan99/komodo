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

import com.kadware.em2200.minalib.AbsoluteModule;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_JumpInstructions extends Test_InstructionProcessor {

    @Test
    public void jump_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START*",
            "          NOP",
            "          J         TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(3, ip.getLatestStopDetail());
    }

    @Test
    public void jump_normal_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "",
            "$(1),START*",
            "          NOP",
            "          J         TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
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

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(3, ip.getLatestStopDetail());
    }

    @Test
    public void jump_indexed_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "",
            "$(1),START*",
            "          NOP",
            "          LXM,U     X5,3",
            "          J         TARGET,X5",
            "",
            "TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "          HALT      3 . Jump here",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
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

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(3, ip.getLatestStopDetail());
    }

    @Test
    public void jump_key_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START*",
            "          NOP       0",
            "          LXM,U     X5,3",
            "          LXI,U     X5,1",
            //TODO fix minalib to handle JK properly - for now, we'll hard code that dang thing
//            "          JK        TARGET,*X5 . Will not jump, will drop through",
            "          $GFORM 6,074,4,04,4,01,4,X5,1,1,1,0,16,TARGET+3",
            "",
            "TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01_000004L, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void haltJump_74_05_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START*",
            "          NOP",
            "          HJ        TARGET",
            "          HALT      0",
            "          HALT      1",
            "          HALT      2",
            "TARGET",
            "          HALT      3",
            "          HALT      4",
            "          HALT      5",
            "          HALT      6",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(3, ip.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_normal_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, ip.getLatestStopReason());
        assertEquals(absoluteModule._startingAddress + 4, ip.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void haltJump_74_15_05_normal_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
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

        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, ip.getLatestStopReason());
        assertEquals(absoluteModule._startingAddress + 4, ip.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void haltJump_74_15_05_pp1_basic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        ExtMainStorageProcessor msp = new ExtMainStorageProcessor("MSP0", (short) 1, 8 * 1024 * 1024);
        InventoryManager.getInstance().addMainStorageProcessor(msp);

        establishBankingEnvironment(ip, msp);
        loadBanks(ip, msp, absoluteModule, 7);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);
        dReg.setProcessorPrivilege(1);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_pp1_extended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "",
            "$(1),START*",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
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
        dReg.setProcessorPrivilege(1);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(absoluteModule._startingAddress);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    @Test
    public void storeLocationAndJump(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  01000   L,U     A0,0
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0)).getW(),      //  01001   L,U     A1,0
            (new InstructionWord(072,  01, 0, 0, 0, 0, 01005)).getW(),  //  01002   SLJ     01005
            (new InstructionWord(010, 016, 1, 0, 0, 0, 5)).getW(),      //  01003   L,U     A1,5
            //???? IAR is not valid for Basic Mode - do something different here, and everywhere we do IAR/BasicMode
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  01004   IAR     d,x,b

            //  Subroutine
            (new InstructionWord(074, 06,  0, 0, 0, 0, 0)).getW(),      //  01005   NOP
            (new InstructionWord(010, 016, 0, 0, 0, 0, 5)).getW(),      //  01006   L,U     A0,5
            (new InstructionWord(074, 004, 0, 0, 0, 1, 01005)).getW(),  //  01007   J       *01005
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        LoadBankInfo infos[] = { codeInfo };
        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(05l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void loadModifierAndJump(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  00  L,U     A0,0
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0)).getW(),      //  01  L,U     A1,0
            (new InstructionWord(074, 013, 013, 0, 0, 0, 05)).getW(),   //  02  LMJ     X11,05
            (new InstructionWord(010, 016, 1, 0, 0, 0, 5)).getW(),      //  03  L,U     A1,5
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  04  IAR     d,x,b

            //  Subroutine
            (new InstructionWord(010, 016, 0, 0, 0, 0, 5)).getW(),      //  05  L,U     A0,5
            (new InstructionWord(074, 015, 04, 013, 0, 0, 0)).getW(),   //  06  J       0,X11
        };

        long[][] sourceData = { code };

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

        assertEquals(05l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  00  L,U     A0,0
            (new InstructionWord(010, 016, 1, 0, 0, 0, 1)).getW(),      //  01  L,U     A1,1
            (new InstructionWord(074,  00, 0, 0, 0, 0, 04)).getW(),     //  02  JZ      A0,04
            (new InstructionWord(010, 016, 0, 0, 0, 0, 077)).getW(),    //  03  L,U     A0,077 . shouldn't get here
            (new InstructionWord(074,  00, 1, 0, 0, 0, 06)).getW(),     //  04  JZ      A1,06
            (new InstructionWord(010, 016, 1, 0, 0, 0, 077)).getW(),    //  05  L,U     A1,077 . should get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  06  IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(077l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void doubleJumpZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  000 L,U     A0,0
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0)).getW(),      //  001 L,U     A1,0
            (new InstructionWord(010, 016, 2, 0, 0, 0, 0)).getW(),      //  002 L,U     A2,0
            (new InstructionWord(010, 016, 3, 0, 0, 0, 1)).getW(),      //  003 L,U     A2,1
            (new InstructionWord(071, 016, 0, 0, 0, 0, 06)).getW(),     //  004 DJZ     A0,06
            (new InstructionWord(010, 016, 0, 0, 0, 0, 077)).getW(),    //  005 L,U     A0,077 . shouldn't get here
            (new InstructionWord(071, 016, 2, 0, 0, 0, 010)).getW(),    //  006 DJZ     A2,010
            (new InstructionWord(010, 016, 2, 0, 0, 0, 077)).getW(),    //  007 L,U     A2,077 . should get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  010 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(077l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void jumpNonZero(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  00  L,U     A0,0
            (new InstructionWord(010, 016, 1, 0, 0, 0, 1)).getW(),      //  01  L,U     A1,1
            (new InstructionWord(074,  01, 0, 0, 0, 0, 04)).getW(),     //  02  JNZ     A0,04
            (new InstructionWord(010, 016, 0, 0, 0, 0, 077)).getW(),    //  03  L,U     A0,077 . should get here
            (new InstructionWord(074,  01, 1, 0, 0, 0, 06)).getW(),     //  04  JNZ     A1,06
            (new InstructionWord(010, 016, 1, 0, 0, 0, 077)).getW(),    //  05  L,U     A1,077 . should not get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  06  IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(077l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpPositive(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  00  L,U     A0,0
            (new InstructionWord(010, 017, 1, 0, 0777776)).getW(),      //  01  L,XU    A1,0777776
            (new InstructionWord(074,  02, 0, 0, 0, 0, 04)).getW(),     //  02  JP      A0,04
            (new InstructionWord(010, 016, 0, 0, 0, 0, 077)).getW(),    //  03  L,U     A0,077 . shouldn't get here
            (new InstructionWord(074,  02, 1, 0, 0, 0, 06)).getW(),     //  04  JP      A1,06
            (new InstructionWord(010, 016, 1, 0, 0, 0, 077)).getW(),    //  05  L,U     A1,077 . should get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  06  IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_77l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpPositiveAndShift(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 010)).getW(),    //  00  L,U     A0,010
            (new InstructionWord(010, 017, 1, 0, 0777776)).getW(),      //  01  L,XU    A1,0777776
            (new InstructionWord(010, 016, 2, 0, 0, 0, 0)).getW(),      //  02  L,U     A2,0
            (new InstructionWord(010, 016, 3, 0, 0, 0, 0)).getW(),      //  03  L,U     A3,0
            (new InstructionWord(072,  02, 0, 0, 0, 0, 06)).getW(),     //  04  JPS     A0,06
            (new InstructionWord(010, 016, 2, 0, 0, 0, 077)).getW(),    //  05  L,U     A2,077 . shouldn't get here
            (new InstructionWord(072,  02, 1, 0, 0, 0, 010)).getW(),    //  06  JPS     A1,010
            (new InstructionWord(010, 016, 3, 0, 0, 0, 077)).getW(),    //  07  L,U     A3,077 . should get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  010 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(020l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void jumpNegative(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  00  L,U     A0,0
            (new InstructionWord(010, 017, 1, 0, 0777776)).getW(),      //  01  L,XU    A1,0777777
            (new InstructionWord(074,  03, 0, 0, 0, 0, 04)).getW(),     //  02  JN      A0,04
            (new InstructionWord(010, 016, 0, 0, 0, 0, 077)).getW(),    //  03  L,U     A0,077 . should get here
            (new InstructionWord(074,  03, 1, 0, 0, 0, 06)).getW(),     //  04  JN      A1,06
            (new InstructionWord(010, 016, 1, 0, 0, 0, 077)).getW(),    //  05  L,U     A1,077 . should not get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  06  IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(077l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777776l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNegativeAndShift(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 010)).getW(),    //  00  L,U     A0,010
            (new InstructionWord(010, 017, 1, 0, 0777776)).getW(),      //  01  L,XU    A1,0777776
            (new InstructionWord(010, 016, 2, 0, 0, 0, 0)).getW(),      //  02  L,U     A2,0
            (new InstructionWord(010, 016, 3, 0, 0, 0, 0)).getW(),      //  03  L,U     A3,0
            (new InstructionWord(072,  03, 0, 0, 0, 0, 06)).getW(),     //  04  JNS     A0,06
            (new InstructionWord(010, 016, 2, 0, 0, 0, 077)).getW(),    //  05  L,U     A2,077 . should get here
            (new InstructionWord(072,  03, 1, 0, 0, 0, 010)).getW(),    //  06  JNS     A1,010
            (new InstructionWord(010, 016, 3, 0, 0, 0, 077)).getW(),    //  07  L,U     A3,077 . should not get here though
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  010 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(020l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void jumpGreaterAndDecrement(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0, 0, 010)).getW(),    //  000 L,U     A0,010
            (new InstructionWord(010, 017, 1, 0, 0777776)).getW(),      //  001 L,XU    A1,0777776
            (new InstructionWord(010, 016, 2, 0, 0, 0, 0)).getW(),      //  002 L,U     A2,0
            (new InstructionWord(070, 0, 015, 0, 0, 0, 007)).getW(),    //  003 JGD     A1,007 . should not happen (A1 is 015)
            (new InstructionWord(070, 0, 016, 0, 0, 0, 007)).getW(),    //  004 JGD     A2,007 . should not happen (A2 is 016)
            (new InstructionWord(014, 016, 2, 0, 0, 0, 02)).getW(),     //  005 A,U     A2,2   . should happen 9 times
            (new InstructionWord(070, 0, 014, 0, 0, 0, 005)).getW(),    //  006 JGD     A0,005 . should happen 8 times (A0 is 014)
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  007 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(0_777777_777776l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_777777_777775l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(021, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
    }

    @Test
    public void jumpModifierGreaterAndIncrement_basic(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            //  test positive case (jump taken)
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0)).getW(),      //  01000   L,U     A0,0        . initial value
            (new InstructionWord(046, 016, 0, 0, 0, 0, 2)).getW(),      //  01001   LXI,U   X0,02       . set up X0 so we take a jump
            (new InstructionWord(026, 016, 0, 0, 0, 0, 02)).getW(),     //  01002   LXM,U   X0,02       .
            (new InstructionWord(074, 012, 0, 0, 0, 0, 01005)).getW(),  //  01003   JMGI    X0,01005    . should take this
            (new InstructionWord(010, 016, 0, 0, 0, 0, 1)).getW(),      //  01004   L,U     A0,1        . should not happen

            //  test negative case (no jump taken)
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0)).getW(),      //  01005   L,U     A1,0        . initial value
            (new InstructionWord(046, 016, 1, 0, 0, 0, 2)).getW(),      //  01006   LXI,U   X1,02       . set up X1 so we do not take a jump
            (new InstructionWord(026, 016, 1, 0, 0, 0, 0)).getW(),      //  01007   LXM,U   X1,0        .
            (new InstructionWord(074, 012, 1, 0, 0, 0, 01012)).getW(),  //  01010   JMGI    X1,01012    . should not take this
            (new InstructionWord(010, 016, 1, 0, 0, 0, 1)).getW(),      //  01011   L,U     A1,1        . should happen

            //  test positive case (jump taken to indexed destination, X(a) == X(x)
            (new InstructionWord(010, 016, 2, 0, 0, 0, 0)).getW(),      //  01012   L,U     A2,0        . initial value
            (new InstructionWord(046, 016, 2, 0, 0, 0, 1)).getW(),      //  01013   LXI,U   X2,01       . set up X2 so we take a jump
            (new InstructionWord(026, 016, 2, 0, 0, 0, 01017)).getW(),  //  01014   LXM,U   X2,01017    .   to zero, indexed by X2
            (new InstructionWord(074, 012, 2, 2, 1, 0, 0)).getW(),      //  01015   JMGI    X2,0,*X2    . should take this
            (new InstructionWord(010, 016, 2, 0, 0, 0, 1)).getW(),      //  01016   L,U     A2,1        . should not happen

            //  test positive case (jump taken to indexed destination, X(a) != X(x)
            (new InstructionWord(010, 016, 3, 0, 0, 0, 0)).getW(),      //  01017   L,U     A3,0        . initial value
            (new InstructionWord(046, 017, 3, 0, 0777776l)).getW(),     //  01020   LXI,XU  X3,0777776  . set up X3 so we take a jump
            (new InstructionWord(026, 016, 3, 0, 0, 0, 010)).getW(),    //  01021   LXM,U   X3,010      .
            (new InstructionWord(046, 016, 4, 0, 0, 0, 1)).getW(),      //  01022   LXI,U   X4,01       . set up X4 so we take a jump
            (new InstructionWord(026, 016, 4, 0, 0, 0, 01026)).getW(),  //  01023   LXM,U   X4,01026    .    to zero, indexed by X4
            (new InstructionWord(074, 012, 3, 4, 1, 0, 0)).getW(),      //  01024   JMGI    X3,0,*X4    . should take this
            (new InstructionWord(010, 016, 3, 0, 0, 0, 1)).getW(),      //  01025   L,U     A3,1        . should not happen

            //  this is a bad way to stop - find a better way for basic mode
            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  01026 IAR       d,x,b
        };

        LoadBankInfo codeInfo = new LoadBankInfo(code);
        codeInfo._lowerLimit = 01000;

        ExtInstructionProcessor ip = new ExtInstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI);
        InventoryManager.getInstance().addInstructionProcessor(ip);
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();

        LoadBankInfo infos[] = { codeInfo };
        loadBanks(ip, msp, 12, infos);

        DesignatorRegister dReg = ip.getDesignatorRegister();
        dReg.setQuarterWordModeEnabled(true);
        dReg.setBasicModeEnabled(true);

        ProgramAddressRegister par = ip.getProgramAddressRegister();
        par.setProgramCounter(01000);

        startAndWait(ip);

        InventoryManager.getInstance().deleteProcessor(ip.getUPI());
        InventoryManager.getInstance().deleteProcessor(msp.getUPI());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000004l, ip.getGeneralRegister(GeneralRegisterSet.X0).getW());

        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
        assertEquals(0_000002_000002l, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        assertEquals(0_000001_001020l, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());

        assertEquals(0l, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
        assertEquals(0_777776_000007l, ip.getGeneralRegister(GeneralRegisterSet.X3).getW());
        assertEquals(0_000001_001027l, ip.getGeneralRegister(GeneralRegisterSet.X4).getW());
    }

    @Test
    public void jumpCarry(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0l)).getW(),           //  000 LA,U    A0,0
            (new InstructionWord(014, 017, 0, 0, 01l)).getW(),          //  001 AA,XU   A0,1        . Does not generate carry
            (new InstructionWord(074, 014, 04, 0, 0, 0, 04)).getW(),    //  002 JC      04          . Should not jump
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A0,0777     . Should happen

            (new InstructionWord(010, 016, 1, 0, 0, 0, 02)).getW(),     //  004 LA,U    A1,2
            (new InstructionWord(014, 017, 1, 0, 0777776)).getW(),      //  005 AA,XU   A1,0777776  . Does generate carry
            (new InstructionWord(074, 014, 04, 0, 0, 0, 010)).getW(),   //  006 JC      010         . Should jump
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A1,0777     . Should not happen

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  004 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(0777l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNoCarry(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0l)).getW(),           //  000 LA,U    A0,0
            (new InstructionWord(014, 017, 0, 0, 01l)).getW(),          //  001 AA,XU   A0,1        . Does not generate carry
            (new InstructionWord(074, 014, 05, 0, 0, 0, 04)).getW(),    //  002 JNC     04          . Should jump
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A0,0777     . Should not happen

            (new InstructionWord(010, 016, 1, 0, 0, 0, 02)).getW(),     //  004 LA,U    A1,2
            (new InstructionWord(014, 017, 1, 0, 0777776)).getW(),      //  005 AA,XU   A1,0777776  . Does generate carry
            (new InstructionWord(074, 014, 05, 0, 0, 0, 010)).getW(),   //  006 JNC     010         . Should not jump
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A1,0777     . Should happen

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  004 IAR     d,x,b
        };

        long[][] sourceData = { code };

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

        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0777l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpOverflow(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_377777_777777l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0l)).getW(),           //  000 LA,U    A0,0
            (new InstructionWord(014, 017, 0, 0, 01l)).getW(),          //  001 AA,XU   A0,1        . Does not generate overflow
            (new InstructionWord(074, 014, 00, 0, 0, 0, 04)).getW(),    //  002 JO      04          . Should not jump
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A0,0777     . Should happen

            (new InstructionWord(010, 0, 1, 0, 0, 0, 1, 0)).getW(),     //  004 LA      A1,0,,B1
            (new InstructionWord(014, 0, 1, 0, 0, 0, 1, 0)).getW(),     //  005 AA      A1,0,,B1    . Does generate overflow
            (new InstructionWord(074, 014, 00, 0, 0, 0, 010)).getW(),   //  006 JO      010         . Should jump
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A1,0777     . Should not happen

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  004 IAR     d,x,b
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

        assertEquals(0777l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertNotEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jumpNoOverflow(
    ) throws MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        long[] data = {
            0_377777_777777l,
        };

        long[] code = {
            (new InstructionWord(010, 016, 0, 0, 0l)).getW(),           //  000 LA,U    A0,0
            (new InstructionWord(014, 017, 0, 0, 01l)).getW(),          //  001 AA,XU   A0,1        . Does not generate overflow
            (new InstructionWord(074, 015, 00, 0, 0, 0, 04)).getW(),    //  002 JNO     04          . Should jump
            (new InstructionWord(010, 016, 0, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A0,0777     . Should not happen

            (new InstructionWord(010, 0, 1, 0, 0, 0, 1, 0)).getW(),     //  004 LA      A1,0,,B1
            (new InstructionWord(014, 0, 1, 0, 0, 0, 1, 0)).getW(),     //  005 AA      A1,0,,B1    . Does generate overflow
            (new InstructionWord(074, 015, 00, 0, 0, 0, 010)).getW(),   //  006 JNO     010         . Should not jump
            (new InstructionWord(010, 016, 1, 0, 0, 0, 0777)).getW(),   //  003 LA,U    A1,0777     . Should happen

            (new InstructionWord(073, 017, 06, 0, 0, 0, 0 ,0)).getW(),  //  004 IAR     d,x,b
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

        assertEquals(01l, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0777l, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    //TODO Need unit tests for JDF, JNDF, JFO, JNFO, JFU, JNFU

    //TODO  need tests for jumps to invalid destinations, once we can handle interrupts
}
