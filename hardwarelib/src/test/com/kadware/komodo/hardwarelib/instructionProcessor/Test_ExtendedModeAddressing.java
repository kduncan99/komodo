/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ExtendedModeAddressing extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void immediateUnsigned_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,U      A0,01000",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,01000",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_ExtendedMode(
    ) throws Exception {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,0777777",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_ExtendedMode(
    ) throws Exception {
        //  Negative zero is converted to positive zero before sign-extension, per hardware docs
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LA,XU     A0,-1",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0_777777_777776L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LR,U      R5,01234",
            "          LA        A0,R5",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2),DATA",
            "          01122,03344,05566",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LBU       B2,DATABDI",
            "          LA        A0,DATA,,B2",
            "          HALT      0",
            "",
            "DESREG    + 0,0     . PP=0, ExtMode, Normal Regs",
            "DATABDI   + LBDIREF$+DATA,0",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(0_112233_445566L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LR,U      R5,01234",
            "          LXM,U     X1,4",
            "          LXI,U     X1,2",
            "          LA        A0,R1,*X1",
            "          HALT      0",
            "",
            "DESREG    + 014,0   . PP=3, ExtMode, Normal Regs",
            "          $END      START"
        };

        buildDualBank(source);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        assertEquals(0_000002_000006L, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void storage_indexed_18BitModifier_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA1     0",
            "          01",
            "          0",
            "          0",
            "          02",
            "          0",
            "          0",
            "          03",
            "          0",
            "          0",
            "          05",
            "          0",
            "          0",
            "          010",
            "",
            "$(2),DATA2 . for auto-increment testing",
            "          $RES 8",
            "",
            "$(4),DATA3 . for X0 testing",
            "          $RES 8",
            "",
            "$(6),DATA4 . for non-auto-increment testing",
            "          $RES 8",
            "",
            "$(1),START",
            "          LD        DESREG",
            "          LBU       B2,DATA1BDI",
            "          LBU       B3,DATA2BDI",
            "          LBU       B4,DATA3BDI",
            "          LBU       B5,DATA4BDI",
            "",
            "          LXM,U     X5,1",
            "          LXI,U     X5,3",
            "          LXM,U     X7,0",
            "          LXI,U     X7,1",
            "          LXM,U     X0,1 . should do nothing",
            "          LXI,U     X0,1 . as above",
            "          LXM,U     X1,1",
            "          LXI,U     X1,0",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          SA        A3,DATA3,*X0,B4",
            "          SA        A3,DATA4,*X1,B5",
            "",
            "          HALT      0",
            "",
            "DESREG    + 0,0     . PP=0, ExtMode, Normal Regs",
            "DATA1BDI  + LBDIREF$+DATA1,0",
            "DATA2BDI  + LBDIREF$+DATA2,0",
            "DATA3BDI  + LBDIREF$+DATA3,0",
            "DATA4BDI  + LBDIREF$+DATA4,0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());

        assertEquals(0_000001_000001L, ip.getExecOrUserXRegister(GeneralRegisterSet.X0).getW());
        assertEquals(0_000000_000001L, ip.getExecOrUserXRegister(GeneralRegisterSet.X1).getW());
        assertEquals(0_000003_000020L, ip.getExecOrUserXRegister(GeneralRegisterSet.X5).getW());
        assertEquals(0_000001_000005L, ip.getExecOrUserXRegister(GeneralRegisterSet.X7).getW());

        long[] bank3Data = getBankByBaseRegister(ip, 3);
        assertEquals(01, bank3Data[0]);
        assertEquals(02, bank3Data[1]);
        assertEquals(03, bank3Data[2]);
        assertEquals(05, bank3Data[3]);
        assertEquals(010, bank3Data[4]);

        long[] bank4Data = getBankByBaseRegister(ip, 4);
        assertEquals(010, bank4Data[0]);
        assertEquals(0, bank4Data[1]);
        assertEquals(0, bank4Data[2]);
        assertEquals(0, bank4Data[3]);

        long[] bank5Data = getBankByBaseRegister(ip, 5);
        assertEquals(0, bank5Data[0]);
        assertEquals(010, bank5Data[1]);
        assertEquals(0, bank5Data[2]);
        assertEquals(0, bank5Data[3]);
    }

    @Test
    public void storage_indexed_24BitModifier_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      . Bank 100004, B2",
            "DATA1     0",
            "          01",
            "          0",
            "          0",
            "          02",
            "          0",
            "          0",
            "          03",
            "          0",
            "          0",
            "          05",
            "          0",
            "          0",
            "          010",
            "",
            "$(2)      . Bank 100006, B3",
            "DATA2     $RES 8",
            "",
            "$(1)      . Bank 100005, B0",
            "START",
            "          LD        DESREG,,B0",
            "          LBU       B2,DATA1BDI,,B0",
            "          LBU       B3,DATA2BDI,,B0",
            "",
            "          LX,U      X5,1",
            "          LXSI,U    X5,03",
            "          LX,U      X7,0",
            "          LXSI,U    X7,01",
            "",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "          LA        A3,DATA1,*X5,B2",
            "          SA        A3,DATA2,*X7,B3",
            "",
            "          HALT      0",
            "",
            "DESREG    + 000100,0 . PP=0, ExtMode, Normal Regs, 24-bit XMods",
            "DATA1BDI  + LBDIREF$+DATA1,0",
            "DATA2BDI  + LBDIREF$+DATA2,0",
            "",
            "          $END      START",
        };

        buildMultiBank(source, false, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        long[] bankData = getBankByBaseRegister(ip, 3);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void execRegisterSelection_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(1)      . Bank 100005, B0",
            "START",
            "          LD        DESREG,,B0",
            "          LA,U      EA5,01",
            "          LX,U      EX5,05",
            "          LR,U      ER5,077",
            "          HALT      0",
            "",
            "DESREG    + 000001,0 . PP=0, ExtMode, Exec Regs",
            "",
            "          $END      START",
        };

        buildMultiBank(source, false, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        ip.getDesignatorRegister().setProcessorPrivilege(0);
        Assert.assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        Assert.assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        Assert.assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }

    @Test
    public void referenceViolationGAPExecute_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1,2",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINIT",
            "",
            "          KCHG      NEWKEY,,B0",
            "          CALL      CALLADDR,,B0              . should fail on execute privilege",
            "                                              . since the dbank is not executable",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1   + 000001,000000             . PP=0, ExtMode, Exec Regs",
            "DESREG2   + 000000,000000             . PP=0, ExtMode, Normal Regs",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "NEWKEY    + 0600777,0                 . ring=3, domain=0777, Desreg per DESREG2",
            "                                      . to force GAP instead of SAP",
            "CALLADDR  + LBDIREF$+CALLFUNC,CALLFUNC",
            "",
            "$(2)      . Code, BDI 100006",
            "CALLFUNC  .",
            "          HALT      0 . shouldn't get here either",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationGAPRead_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1,2",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINIT",
            "",
            "          KCHG      NEWKEY,,B0",
            "          LA        A5,DESREG1,,B0    . should fail due to GAP having no read access",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1   + 000001,000000             . PP=0, ExtMode, Exec Regs",
            "DESREG2   + 000000,000000             . PP=0, ExtMode, Normal Regs",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "NEWKEY    + 0600777,0                 . ring=3, domain=0777, Desreg per DESREG2",
            "                                      . to force GAP instead of SAP",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationGAPWrite_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1,2",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINIT",
            "",
            "          KCHG      NEWKEY,,B0",
            "          SA        A5,SCRATCH,,B0    . should fail due to GAP having no write access",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1   + 000001,000000             . PP=0, ExtMode, Exec Regs",
            "DESREG2   + 000000,000000             . PP=0, ExtMode, Normal Regs",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "NEWKEY    + 0600777,0                 . ring=3, domain=0777, Desreg per DESREG2",
            "                                      . to force GAP instead of SAP",
            "SCRATCH   $RES 1",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceOutOfLimits_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINIT",
            "",
            "          LD        DESREG3",
            "          LA        A0,07777,B0",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1   + 000001,000000 . PP=0, ExtMode, Exec Regs",
            "DESREG2   + 000000,000000 . PP=0, ExtMode, Normal Regs",
            "DESREG3   + 000014,000000 . PP=3, ExtMode, Normal Regs",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationSAPExecute_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINITADDR",
            "",
            "          . Change SAP for bankdesc for 100006 to disallow execute",
            "          . The bankdesc is in 8-word entry 6 (zero-biased) in",
            "          . BDT level 1, which is based on B17.",
            "          . This bank would normally have the execute bit set.",
            "          LXM,U     X1,06*8",
            "          LA        A0,0,X1,B17",
            "          AND       A0,MASK,,B0",
            "          SA        A1,0,X1,B17",
            "",
            "          . Change our key to 3:0777 and try executing SUB (should fail)",
            "          KCHG      KEY,,B0",
            "          CALL      SUBADDR,,B0",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1    + 000001,000000 . PP=0, ExtMode, Exec Regs",
            "DESREG2    + 000000,000000 . PP=0, ExtMode, Normal Regs",
            "RCSBDI     + LBDIREF$+RCSTACK,0",
            "IHINITADDR + LBDIREF$+IH$INIT,IH$INIT",
            "SUBADDR    + LBDIREF$+SUB,SUB",
            "KEY        + 0600777,0     . Key in H1, DesReg.H1 in H2",
            "MASK       + 0737777,0777777",
            "",
            "$(3)      . Code, BDI 100006",
            "SUB       HALT      0 . shouldn't get here either",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationSAPRead_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(4)      . Data, BDI 100006",
            "DATA      HALT      0 . shouldn't get here either",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINITADDR",
            "",
            "          . Change SAP for bankdesc for 100006 to disallow read",
            "          . The bankdesc is in 8-word entry 6 (zero-biased) in",
            "          . BDT level 1, which is based on B17.",
            "          . This bank would normally have the read bit set.",
            "          LXM,U     X1,06*8",
            "          LA        A0,0,X1,B17",
            "          AND       A0,MASK,,B0",
            "          SA        A1,0,X1,B17",
            "",
            "          . Base 100006 on B2",
            "          LBU       B2,DATABDI,,B0",
            "",
            "          . Change our key to 3:0777 and try writing to DATA (should fail)",
            "          KCHG      KEY,,B0",
            "          SA        A0,DATA,,B2",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1    + 000001,000000 . PP=0, ExtMode, Exec Regs",
            "DESREG2    + 000000,000000 . PP=0, ExtMode, Normal Regs",
            "RCSBDI     + LBDIREF$+RCSTACK,0",
            "DATABDI    + LBDIREF$+DATA,0",
            "IHINITADDR + LBDIREF$+IH$INIT,IH$INIT",
            "KEY        + 0600777,0     . Key in H1, DesReg.H1 in H2",
            "MASK       + 0757777,0777777",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationSAPWrite_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINITADDR",
            "",
            "          . Change our key to 3:0777 and try writing to B0." +
            "          . The code bank is always write-disabled, so this should fail.",
            "          KCHG      KEY,,B0",
            "          SA        A0,$,,B0",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1    + 000001,000000 . PP=0, ExtMode, Exec Regs",
            "DESREG2    + 000000,000000 . PP=0, ExtMode, Normal Regs",
            "RCSBDI     + LBDIREF$+RCSTACK,0",
            "IHINITADDR + LBDIREF$+IH$INIT,IH$INIT",
            "KEY        + 0600777,0     . Key in H1, DesReg.H1 in H2",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void referenceViolationUnbasedBaseRegisterRef_ExtendedMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(2)      . RCS, BDI 100005",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      . Code, BDI 100004",
            "START",
            "          . Set up RCS",
            "          LD        DESREG1",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . Set up interrupt handlers",
            "          LD        DESREG2",
            "          CALL      IHINIT",
            "",
            "          LD        DESREG3",
            "          LBU       B5,ZEROBDI,,B0",
            "          LA        A0,01000,,B5",
            "          HALT      0 . shouldn't get here",
            "",
            "DESREG1   + 000001,000000 . PP=0, ExtMode, Exec Regs",
            "DESREG2   + 000000,000000 . PP=0, ExtMode, Normal Regs",
            "DESREG3   + 000014,000000 . PP=3, ExtMode, Normal Regs",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "ZEROBDI   + 0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }
}
