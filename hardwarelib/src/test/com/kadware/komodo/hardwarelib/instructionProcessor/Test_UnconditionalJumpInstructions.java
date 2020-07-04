/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.*;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.*;
import com.kadware.komodo.kex.kasm.*;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_UnconditionalJumpInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    //TODO
//    @Test
//    public void jump_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          NOP",
//            "          J         TARGET",
//            "          HALT      0",
//            "          HALT      1",
//            "          HALT      2",
//            "TARGET",
//            "          HALT      3",
//            "          HALT      4",
//            "          HALT      5",
//            "          HALT      6",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
//    }
//
    //TODO
//    @Test
//    public void jump_bankSelection(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "$(0) . this will be 0600005 based on B14", // primary DBank for DB31=0
//            "DATA      + 1",
//            "",
//            "$(1) . this will be 0600004 based on B12", // primary IBank for DB31=0,
//            "START$*",
//            "          LA        A1,DATA . should get value from $(0)",
//            "          J         TARGET",
//            "",
//            "DONE",
//            "          HALT      0",
//            "",
//            "$(2) . this will be 0600007 based on B15", // primary DBank for DB31=1
//            "          + 2 . will be linked so as to overlap DATA in $(0)",
//            "",
//            "$(3) . this will be 0600006 based on B13", // primary IBank for DB31=1
//            "TARGET",
//            "          LA        A2,DATA . should get value from $(2)",
//            "          J         DONE",
//            "          HALT 077",
//        };
//
//        //  Special construction of the absolute
//        Assembler.Option[] asmOptions = {};
//        Assembler asm = new Assembler();
//        OldRelocatableModule relModule = asm.assemble("TEST", source, asmOptions);
//
//        Linker.LCPoolSpecification[] bank4PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 1),
//        };
//
//        Linker.LCPoolSpecification[] bank5PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 0),
//        };
//
//        Linker.LCPoolSpecification[] bank6PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 3),
//        };
//
//        Linker.LCPoolSpecification[] bank7PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 2),
//        };
//
//        AccessInfo lock = new AccessInfo((short)3, 100);
//        AccessPermissions iBankPerms = new AccessPermissions(true, true, false);
//        AccessPermissions dBankPerms = new AccessPermissions(false, true, false);
//
//        Linker.BankDeclaration[] bankDeclarations = {
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(4)
//                .setBankName("I1")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(12)
//                .setGeneralAccessPermissions(iBankPerms)
//                .setSpecialAccessPermissions(iBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank4PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(5)
//                .setBankName("D1")
//                .setStartingAddress(022000)
//                .setInitialBaseRegister(14)
//                .setGeneralAccessPermissions(dBankPerms)
//                .setSpecialAccessPermissions(dBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank5PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(6)
//                .setBankName("I2")
//                .setStartingAddress(02000)
//                .setInitialBaseRegister(13)
//                .setGeneralAccessPermissions(iBankPerms)
//                .setSpecialAccessPermissions(iBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank6PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(7)
//                .setBankName("D2")
//                .setStartingAddress(022000)
//                .setInitialBaseRegister(15)
//                .setGeneralAccessPermissions(dBankPerms)
//                .setSpecialAccessPermissions(dBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank7PoolSpecs)
//                .build(),
//        };
//
//        Linker.Option[] linkerOptions = {};
//        Linker linker = new Linker();
//        AbsoluteModule absoluteModule = linker.link("TEST", bankDeclarations, 0, linkerOptions);
//
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//
//        assertEquals(1, processors._instructionProcessor.getExecOrUserARegister(1).getW());
//        assertEquals(2, processors._instructionProcessor.getExecOrUserARegister(2).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_indexed_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          NOP",
//            "          LXI,U     X3,1",
//            "          LXM,U     X3,5",
//            "          J         TARGET,*X3",
//            "          HALT      077",
//            "",
//            "TARGET",
//            "          HALT      076",
//            "          HALT      075",
//            "          HALT      074",
//            "          HALT      073",
//            "          HALT      072",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(01_000006L, processors._instructionProcessor.getGeneralRegister(3).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_indirect_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          J         *TARGET2",
//            "          HALT      077",
//            "",
//            "TARGET1",
//            "          J         DONE",
//            "          HALT      075",
//            "",
//            "TARGET2",
//            "          J         *TARGET1",
//            "          HALT      076",
//            "",
//            "DONE",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//    }
//
    //TODO
//    @Test
//    public void jump_indexed_indirect_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,2",
//            "          LXI,U     X3,1",
//            "          LXM,U     X3,1",
//            "          J         *TARGET2,*X2",
//            "          HALT      077",
//            "",
//            "TARGET1",
//            "          HALT      073",
//            "          J         DONE",
//            "          HALT      072",
//            "",
//            "TARGET2",
//            "          HALT      076",
//            "          HALT      075",
//            "          J         *TARGET1,*X3",
//            "          HALT      074",
//            "",
//            "DONE",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(01_000003, processors._instructionProcessor.getGeneralRegister(2).getW());
//        assertEquals(01_000002, processors._instructionProcessor.getGeneralRegister(3).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          NOP",
//            "          J         TARGET",
//            "          HALT      0",
//            "          HALT      1",
//            "          HALT      2",
//            "          $RES      040000 . use large jump to ensure we use U, not D",
//            "TARGET",
//            "          HALT      3",
//            "          HALT      4",
//            "          HALT      5",
//            "          HALT      6",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
//    }
//
    //TODO
//    @Test
//    public void jump_indexed_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LXI,U     X5,2",
//            "          LXM,U     X5,3",
//            "          J         TARGET,*X5",
//            "",
//            "TARGET",
//            "          HALT      0",
//            "          HALT      1",
//            "          HALT      2",
//            "          HALT      3 . Jump here",
//            "          HALT      4",
//            "          HALT      5",
//            "          HALT      6",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(02_000005, processors._instructionProcessor.getGeneralRegister(5).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_key_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LXM,U     X5,3",
//            "          LXI,U     X5,1",
//            "          JK        TARGET,*X5 . Will not conditionalJump, will drop through",
//            "",
//            "TARGET",
//            "          HALT      0",
//            "          HALT      1",
//            "          HALT      2",
//            "          HALT      3",
//            "          HALT      4",
//            "          HALT      5",
//            "          HALT      6",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(01_000004L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
//    }
//
    //TODO
//    @Test
//    public void haltJump_74_05_normal_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          NOP",
//            "          HJ        TARGET",
//            "          HALT      0",
//            "          HALT      1",
//            "          HALT      2",
//            "TARGET",
//            "          HALT      3",
//            "          HALT      4",
//            "          HALT      5",
//            "          HALT      6",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(3, processors._instructionProcessor.getLatestStopDetail());
//    }
//
    //TODO
//    @Test
//    public void haltJump_74_15_05_normal_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          HLTJ      TARGET",
//            "          LA,U      A0,5",
//            "          HALT      0",
//            "",
//            "TARGET",
//            "          HALT      1",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(absoluteModule._entryPointAddress + 4, processors._instructionProcessor.getProgramAddressRegister().getProgramCounter());
//    }
//
    //TODO
//    @Test
//    public void haltJump_74_15_05_normal_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          HLTJ      TARGET",
//            "          LA,U      A0,5",
//            "          HALT      0",
//            "",
//            "TARGET",
//            "          HALT      1",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(absoluteModule._entryPointAddress + 4, processors._instructionProcessor.getProgramAddressRegister().getProgramCounter());
//    }
//
    //TODO
//    @Test
//    public void haltJump_74_15_05_pp1_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          HLTJ      TARGET . should throw interrupt",
//            "          HALT      077    . should not get here",
//            "TARGET "
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
//    }
//
    //TODO
//    @Test
//    public void haltJump_74_15_05_pp1_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0",
//            "          HLTJ      TARGET",
//            "          LA,U      A0,5",
//            "          HALT      0",
//            "",
//            "TARGET",
//            "          HALT      1",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
//    }
//
//    //  no extended mode version of SLJ
//
    //TODO
//    @Test
//    public void storeLocationAndJump(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0         . set up initial values",
//            "          LA,U      A1,0",
//            "          SLJ       SUBROUTINE",
//            "          $GFORM    6,072,4,01,4,0,4,0,1,0,1,0,16,SUBROUTINE",
//            "          LA,U      A1,5         . change A1 value post-subroutine",
//            "          HALT      0            . done",
//            "",
//            "SUBROUTINE .",
//            "          + 0                    . where return address is stored",
//            "          LA,U      A0,5         . update A0 value",
//            "          J         *SUBROUTINE  . return to caller",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
    //TODO
//    @Test
//    public void storeLocationAndJump_indirect(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0),VECTOR  + SUBROUTINE",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0         . set up initial values",
//            "          LA,U      A1,0",
//            "          SLJ       *VECTOR",
//            "          $GFORM    6,072,4,01,4,0,4,0,1,0,1,0,16,SUBROUTINE",
//            "          LA,U      A1,5         . change A1 value post-subroutine",
//            "          HALT      0            . done",
//            "",
//            "SUBROUTINE .",
//            "          + 0                    . where return address is stored",
//            "          LA,U      A0,5         . update A0 value",
//            "          J         *SUBROUTINE  . return to caller",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
    //TODO
//    @Test
//    public void loadModifierAndJump_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0         . set up initial values",
//            "          LA,U      A1,0",
//            "          LMJ       X11,SUBROUTINE",
//            "          LA,U      A1,5         . change A1 value post-subroutine",
//            "          HALT      0            . done",
//            "",
//            "SUBROUTINE .",
//            "          LA,U      A0,5         . update A0 value",
//            "          J         0,X11        . return to caller",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
    //TODO
//    @Test
//    public void loadModifierAndJump_indirect_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0),VECTOR  + SUBROUTINE",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0         . set up initial values",
//            "          LA,U      A1,0",
//            "          LMJ       X11,*VECTOR",
//            "          LA,U      A1,5         . change A1 value post-subroutine",
//            "          HALT      0            . done",
//            "",
//            "SUBROUTINE .",
//            "          LA,U      A0,5         . update A0 value",
//            "          J         0,X11        . return to caller",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
    //TODO
//    @Test
//    public void loadModifierAndJump_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          LA,U      A0,0         . set up initial values",
//            "          LA,U      A1,0",
//            "          LMJ       X11,SUBROUTINE",
//            "          LA,U      A1,5         . change A1 value post-subroutine",
//            "          HALT      0            . done",
//            "          $RES      020000       . use large jump to ensure we use U, not D",
//            "",
//            "SUBROUTINE .",
//            "          LA,U      A0,5         . update A0 value",
//            "          J         0,X11        . return to caller",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A0).getW());
//        assertEquals(05L, processors._instructionProcessor.getGeneralRegister(GeneralRegisterSet.A1).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_basic_referenceViolation1(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  address out of limits
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          NOP",
//            "          J         050000",
//            "          HALT      077",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
//                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
//    }
//
    //TODO
//    @Test
//    public void jump_basic_referenceViolation2(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        //  address out of limits
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LXI,U     X3,010",
//            "          LXM,U     X3,0100",
//            "          J         0,*X3",
//            "          HALT      077",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
//                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
//        assertEquals(010_000110L, processors._instructionProcessor.getGeneralRegister(3).getW());
//    }
//
    //TODO
//    @Test
//    public void jump_extended_referenceViolation1(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(1),START$*",
//            "          J         START$+02000",
//            "          HALT      077",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor._upiIndex);
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
//        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
//                     processors._instructionProcessor.getLastInterrupt().getShortStatusField());
//    }
}
