/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_LogicalInstructions extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    @Test
    public void logicalANDBasic(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          GOTO      (LBDIREF$+BMSTART,BMSTART)",
            "",
            "          $BASIC",
            "$(3)",
            "          $LIT",
            "BMSTART",
            "          LA        A4,(0777777777123)",
            "          AND,U     A4,0543321",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777777_777123L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        Assert.assertEquals(0_000000_543121L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void logicalANDExtended(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          LA        A4,(0777777777123)",
            "          AND,U     A4,0543321",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777777_777123L, ip.getGeneralRegister(GeneralRegisterSet.A4).getW());
        Assert.assertEquals(0_000000_543121L, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
    }

    @Test
    public void logicalMLUBasic(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          GOTO      (LBDIREF$+BMSTART,BMSTART)",
            "",
            "          $BASIC",
            "$(3)",
            "          $LIT",
            "BMSTART",
            "          LA        A8,(0777777000000)",
            "          LR        R2,(0707070707070)",
            "          MLU       A8,(0000000777777)",
            "          HALT      0",
            "",
            "          $END      START",
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777777_000000L, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        Assert.assertEquals(0_070707_707070L, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void logicalMLUExtended(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          LA        A8,(0777777000000)",
            "          LR        R2,(0707070707070)",
            "          MLU       A8,(0000000777777)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777777_000000L, ip.getGeneralRegister(GeneralRegisterSet.A8).getW());
        Assert.assertEquals(0_070707_707070L, ip.getGeneralRegister(GeneralRegisterSet.A9).getW());
    }

    @Test
    public void logicalORBasic(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          GOTO      (LBDIREF$+BMSTART,BMSTART)",
            "",
            "          $BASIC",
            "$(3)",
            "          $LIT",
            "BMSTART",
            "          LA        A0,(0111111111111)",
            "          OR        A0,(0222222222222)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_111111_111111L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_333333_333333L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void logicalORExtended(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          LA        A0,(0111111111111)",
            "          OR        A0,(0222222222222)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_111111_111111L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_333333_333333L, ip.getGeneralRegister(GeneralRegisterSet.A1).getW());
    }

    @Test
    public void logicalXORBasic(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          GOTO      (LBDIREF$+BMSTART,BMSTART)",
            "",
            "          $BASIC",
            "$(3)",
            "          $LIT",
            "BMSTART",
            "          LA        A2,(0777000777000)",
            "          XOR,H1    A2,(0750750777777)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777000_777000L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        Assert.assertEquals(0_777000_027750L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }

    @Test
    public void logicalXORExtended(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          LA        A2,(0777000777000)",
            "          XOR,H1    A2,(0750750777777)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildSimple(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777000_777000L, ip.getGeneralRegister(GeneralRegisterSet.A2).getW());
        Assert.assertEquals(0_777000_027750L, ip.getGeneralRegister(GeneralRegisterSet.A3).getW());
    }
}
