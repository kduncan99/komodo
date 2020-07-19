/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.klink.BankDeclaration;
import com.kadware.komodo.kex.klink.LCPoolSpecification;
import com.kadware.komodo.kex.klink.LinkOption;
import com.kadware.komodo.kex.klink.LinkType;
import com.kadware.komodo.kex.klink.Linker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_UnconditionalJumpInstructions extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    @Test
    public void jump_basic(
    ) throws Exception {
        String[] source = {
            "          J         TARGET",
            "          HALT      077",
            "          HALT      076",
            "          HALT      075",
            "TARGET",
            "          HALT      0",
            "          HALT      074",
            "          HALT      073",
            "          HALT      072"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
    }

    @Test
    public void jump_bankSelection(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)  . extended mode ibank 0100004 - start here",
            "          $LIT",
            "START",
            "          LD        (000001,000000)     . ext mode, exec regs, pp=0",
            ".",
            "          LD        (0,0)               . ext mode, user regs, pp=0",
            "          GOTO      (LBDIREF$+BMSTART, BMSTART)",
            "",
            "          $BASIC",
            "$(11) . this will be 0100011 based on B12", // primary IBank for DB31=0,
            "          $LIT",
            "BMSTART*",
            "          LBU       B13,(LBDIREF$+TARGET, 0)",
            "          LBU       B14,(LBDIREF$+DATA, 0)",
            "          LBU       B15,(LBDIREF$+OTHERDATA, 0)",
            "          LA        A1,DATA . should get value from $(10)",
            "          J         TARGET",
            "",
            "DONE",
            "          HALT      0",
            "",
            "$(10) . this will be 0100010 based on B14", // primary DBank for DB31=0
            "DATA*     + 1",
            "",
            "$(12) . this will be 0100012 based on B15", // primary DBank for DB31=1
            "OTHERDATA* + 2 . will be linked so as to overlap DATA in $(10)",
            "",
            "$(13) . this will be 0100013 based on B13", // primary IBank for DB31=1
            "TARGET*",
            "          LA        A2,DATA . should get value from $(12)",
            "          J         DONE",
            "          HALT 077",
            "",
            "          $END START"
        };

        //  Special construction of the absolute
        AssemblerOption[] asmOpts = {
            AssemblerOption.EMIT_SOURCE,
            AssemblerOption.EMIT_GENERATED_CODE,
            AssemblerOption.EMIT_MODULE_SUMMARY
        };
        _assembler = new Assembler.Builder().setSource(source)
                                            .setOptions(asmOpts)
                                            .setModuleName("TEST")
                                            .build();
        _assemblerResult = _assembler.assemble();
        Assert.assertFalse(_assemblerResult._diagnostics.hasError());

        LCPoolSpecification[] poolSpecs100004 = {
            new LCPoolSpecification(_assemblerResult._relocatableModule,1)
        };

        LCPoolSpecification[] poolSpecs100010 = {
            new LCPoolSpecification(_assemblerResult._relocatableModule, 10)
        };

        LCPoolSpecification[] poolSpecs100011 = {
            new LCPoolSpecification(_assemblerResult._relocatableModule, 11)
        };

        LCPoolSpecification[] poolSpecs100012 = {
            new LCPoolSpecification(_assemblerResult._relocatableModule, 12)
        };

        LCPoolSpecification[] poolSpecs100013 = {
            new LCPoolSpecification(_assemblerResult._relocatableModule, 13)
        };

        AccessInfo accessInfo = new AccessInfo(0, 0);
        AccessPermissions gap = new AccessPermissions(false, false, false);
        AccessPermissions codeSap = new AccessPermissions(true, true, false);
        AccessPermissions dataSap = new AccessPermissions(false, true, true);
        BankDeclaration.BankDeclarationOption[] extCodeOpts = {
            BankDeclaration.BankDeclarationOption.EXTENDED_MODE,
            BankDeclaration.BankDeclarationOption.WRITE_PROTECT
        };
        BankDeclaration.BankDeclarationOption[] basicCodeOpts = {
            BankDeclaration.BankDeclarationOption.WRITE_PROTECT,
        };
        BankDeclaration.BankDeclarationOption[] dataOpts = {
            BankDeclaration.BankDeclarationOption.DYNAMIC,
            BankDeclaration.BankDeclarationOption.DBANK
        };

        BankDeclaration bd100004 = new BankDeclaration.Builder().setBankLevel(1)
                                                                .setBankDescriptorIndex(4)
                                                                .setBankName("EMIBANK")
                                                                .setAccessInfo(accessInfo)
                                                                .setGeneralAccessPermissions(gap)
                                                                .setSpecialAccessPermissions(codeSap)
                                                                .setPoolSpecifications(poolSpecs100004)
                                                                .setStartingAddress(01000)
                                                                .setOptions(extCodeOpts)
                                                                .build();
        BankDeclaration bd100010 = new BankDeclaration.Builder().setBankLevel(1)
                                                                .setBankDescriptorIndex(010)
                                                                .setBankName("BM-B14")
                                                                .setAccessInfo(accessInfo)
                                                                .setGeneralAccessPermissions(gap)
                                                                .setSpecialAccessPermissions(dataSap)
                                                                .setPoolSpecifications(poolSpecs100010)
                                                                .setStartingAddress(022000)
                                                                .setOptions(dataOpts)
                                                                .build();
        BankDeclaration bd100011 = new BankDeclaration.Builder().setBankLevel(1)
                                                                .setBankDescriptorIndex(011)
                                                                .setBankName("BM-B12")
                                                                .setAccessInfo(accessInfo)
                                                                .setGeneralAccessPermissions(gap)
                                                                .setSpecialAccessPermissions(codeSap)
                                                                .setPoolSpecifications(poolSpecs100011)
                                                                .setStartingAddress(01000)
                                                                .setOptions(basicCodeOpts)
                                                                .build();
        BankDeclaration bd100012 = new BankDeclaration.Builder().setBankLevel(1)
                                                                .setBankDescriptorIndex(012)
                                                                .setBankName("BM-B15")
                                                                .setAccessInfo(accessInfo)
                                                                .setGeneralAccessPermissions(gap)
                                                                .setSpecialAccessPermissions(dataSap)
                                                                .setPoolSpecifications(poolSpecs100012)
                                                                .setStartingAddress(022000)
                                                                .setOptions(dataOpts)
                                                                .build();
        BankDeclaration bd100013 = new BankDeclaration.Builder().setBankLevel(1)
                                                                .setBankDescriptorIndex(013)
                                                                .setBankName("BM-B13")
                                                                .setAccessInfo(accessInfo)
                                                                .setGeneralAccessPermissions(gap)
                                                                .setSpecialAccessPermissions(codeSap)
                                                                .setPoolSpecifications(poolSpecs100013)
                                                                .setStartingAddress(05000)
                                                                .setOptions(basicCodeOpts)
                                                                .build();
        BankDeclaration[] bankDeclarations = {
            bd100004,
            bd100010,
            bd100011,
            bd100012,
            bd100013
        };

        LinkOption[] linkOpts = {
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_LCPOOL_MAP
        };
        Linker linker = new Linker.Builder().setOptions(linkOpts)
                                            .setBankDeclarations(bankDeclarations)
                                            .build();
        _linkResult = linker.link(LinkType.MULTI_BANKED_BINARY);
        Assert.assertEquals(0, _linkResult._errorCount);

        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());

        assertEquals(1, ip.getExecOrUserARegister(1).getW());
        assertEquals(2, ip.getExecOrUserARegister(2).getW());
    }

    @Test
    public void jump_indexed_basic(
    ) throws Exception {
        String[] source = {
            "          LXI,U     X3,1",
            "          LXM,U     X3,5",
            "          J         TARGET,*X3",
            "          HALT      077",
            "",
            "TARGET",
            "          HALT      076",
            "          HALT      075",
            "          HALT      074",
            "          HALT      073",
            "          HALT      072",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01_000006L, ip.getGeneralRegister(3).getW());
    }

    @Test
    public void jump_indirect_basic(
    ) throws Exception {
        String[] source = {
            "          J         *TARGET2",
            "          HALT      077",
            "",
            "TARGET1",
            "          J         DONE",
            "          HALT      075",
            "",
            "TARGET2",
            "          J         *TARGET1",
            "          HALT      076",
            "",
            "DONE",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
    }

    @Test
    public void jump_indexed_indirect_basic(
    ) throws Exception {
        String[] source = {
            "          LXI,U     X2,1",
            "          LXM,U     X2,2",
            "          LXI,U     X3,1",
            "          LXM,U     X3,1",
            "          J         *TARGET2,*X2",
            "          HALT      077",
            "",
            "TARGET1",
            "          HALT      073",
            "          J         DONE",
            "          HALT      072",
            "",
            "TARGET2",
            "          HALT      076",
            "          HALT      075",
            "          J         *TARGET1,*X3",
            "          HALT      074",
            "",
            "DONE",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01_000003, ip.getGeneralRegister(2).getW());
        assertEquals(01_000002, ip.getGeneralRegister(3).getW());
    }

    @Test
    public void jump_extended(
    ) throws Exception {
        String[] source = {
            "          GOTO      (LBDIREF$+TEST, TEST)",
            "",
            "          $INFO 10 7",
            "$(7)      $LIT",
            "TEST",
            "          J         TARGET",
            "          $RES      040000 . use large jump to ensure we use U, not D",
            "TARGET",
            "          HALT      0",
            "          HALT      077",
            "          HALT      076",
            "          HALT      075"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
    }

    @Test
    public void jump_indexed_extended(
    ) throws Exception {
        String[] source = {
            "          LXI,U     X5,2",
            "          LXM,U     X5,3",
            "          J         TARGET,*X5",
            "",
            "TARGET",
            "          HALT      077",
            "          HALT      076",
            "          HALT      075",
            "          HALT      0 . Jump here",
            "          HALT      074",
            "          HALT      073",
            "          HALT      072"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(02_000005, ip.getGeneralRegister(5).getW());
    }

    @Test
    public void jump_key_basic(
    ) throws Exception {
        String[] source = {
            "          LXM,U     X5,3",
            "          LXI,U     X5,1",
            "          JK        TARGET,*X5 . Will not jump, will drop through",
            "",
            "TARGET",
            "          HALT      0",
            "          HALT      077",
            "          HALT      076",
            "          HALT      075",
            "          HALT      074",
            "          HALT      073",
            "          HALT      072"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01_000004L, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
    }

    @Test
    public void haltJump_74_05_normal_basic(
    ) throws Exception {
        String[] source = {
            "          HJ        TARGET",
            "          HALT      077",
            "TARGET",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_normal_basic(
    ) throws Exception {
        String[] source = {
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, ip.getLatestStopReason());
    }

    @Test
    public void haltJump_74_15_05_normal_extended(
    ) throws Exception {
        String[] source = {
            "          GOTO      (LBDIREF$+TEST, TEST)",
            "",
            "          $INFO 10 7",
            "$(7)      $LIT",
            "TEST",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1         . offset 04 from bank start"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.HaltJumpExecuted, ip.getLatestStopReason());
        assertEquals(01004, ip.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void haltJump_74_15_05_pp1_basic(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "",
            "          DR$SETPP  01     . set proc priv = 1",
            "          HLTJ      TARGET . should throw interrupt",
            "          HALT      077    . should not get here",
            "TARGET    HALT      076    . should not get here either"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    @Test
    public void haltJump_74_15_05_pp1_extended(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "",
            "          DR$SETPP  01     . set proc priv = 1",
            "          LA,U      A0,0",
            "          HLTJ      TARGET",
            "          LA,U      A0,5",
            "          HALT      0",
            "",
            "TARGET",
            "          HALT      1",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01016, ip.getLatestStopDetail());
    }

    //  no extended mode version of SLJ

    @Test
    public void storeLocationAndJump(
    ) throws Exception {
        String[] source = {
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          SLJ       SUBROUTINE",
            "          $GFORM    6,072,4,01,4,0,4,0,1,0,1,0,16,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          + 0                    . where return address is stored",
            "          LA,U      A0,5         . update A0 value",
            "          J         *SUBROUTINE  . return to caller"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void storeLocationAndJump_indirect(
    ) throws Exception {
        String[] source = {
            "$(4),VECTOR  + SUBROUTINE",
            "",
            "$(3)",
            "          LBU       B13,(LBDIREF$+VECTOR, 0)",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          SLJ       *VECTOR",
            "          $GFORM    6,072,4,01,4,0,4,0,1,0,1,0,16,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          + 0                    . where return address is stored",
            "          LA,U      A0,5         . update A0 value",
            "          J         *SUBROUTINE  . return to caller"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void loadModifierAndJump_basic(
    ) throws Exception {
        String[] source = {
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          LMJ       X11,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          LA,U      A0,5         . update A0 value",
            "          J         0,X11        . return to caller"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void loadModifierAndJump_indirect_basic(
    ) throws Exception {
        String[] source = {
            "$(4),VECTOR  + SUBROUTINE",
            "",
            "$(3)",
            "          LBU       B13,(LBDIREF$+VECTOR, 0)",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          LMJ       X11,*VECTOR",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "",
            "SUBROUTINE .",
            "          LA,U      A0,5         . update A0 value",
            "          J         0,X11        . return to caller"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void loadModifierAndJump_extended(
    ) throws Exception {
        String[] source = {
            "          CALL      (LBDIREF$+TEST, TEST) . because of the large ibank",
            "",
            "          $INFO 10 5",
            "$(5)      $LIT",
            "TEST",
            "          LA,U      A0,0         . set up initial values",
            "          LA,U      A1,0",
            "          LMJ       X11,SUBROUTINE",
            "          LA,U      A1,5         . change A1 value post-subroutine",
            "          HALT      0            . done",
            "          $RES      020000       . use large jump to ensure we use U, not D",
            "",
            "SUBROUTINE .",
            "          LA,U      A0,5         . update A0 value",
            "          J         0,X11        . return to caller",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(05L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void jump_basic_referenceViolation1(
    ) throws Exception {
        //  address out of limits
        String[] source = {
            "          J         050000",
            "          HALT      077",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void jump_basic_referenceViolation2(
    ) throws Exception {
        //  address out of limits
        String[] source = {
            "          LXI,U     X3,010",
            "          LXM,U     X3,0100",
            "          J         0,*X3",
            "          HALT      077",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
                     ip.getLastInterrupt().getShortStatusField());
        assertEquals(010_000110L, ip.getGeneralRegister(3).getW());
    }

    @Test
    public void jump_extended_referenceViolation1(
    ) throws Exception {
        String[] source = {
            "          J         $+02000",
            "          HALT      077",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();
        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
        assertEquals((ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4) + 1,
                     ip.getLastInterrupt().getShortStatusField());
    }
}
