/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_BasicModeAddressing extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests for addressing modes
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void immediateUnsigned_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LA,U      A0,01000 .",
            "          HALT      0 .",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Positive_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LA,XU     A0,01000",
            "          HALT      0",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01000, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_NegativeZero_BasicMode(
    ) throws Exception {
        String[] source = {
            //  Negative zero is converted to positive zero before sign-extension, per hardware docs
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LA,XU     A0,0777777",
            "          HALT      0",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void immediateSignedExtended_Negative_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LA,XU     A0,-1",
            "          HALT      0",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_777777_777776L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LR,U      R5,01234",
            "          LA        A0,R5",
            "          HALT      0",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void grs_indexed_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LR,U      R5,01234",
            "          LXM,U     X1,4        . Set X modifier to 4 and increment to 2",
            "          LXI,U     X1,2",
            "          LA        A0,R1,*X1   . Use X-reg modifying R1 GRS to get to R5",
            "          HALT      0",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
        Assert.assertEquals(0_000002_000006L, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
    }

    @Test
    public void grs_indirect_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LR,U      R5,01234",
            "          LA        A0,*INDIRECT  . Indirection through INDIRECT",
            "                                  .   will transfer content from R5 to A0",
            "          HALT      0",
            "",
            "INDIRECT  + R5",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01234, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void storage_indexed_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LBU       B13,(LBDIREF$+DATA1,0)",
            "          LBU       B15,(LBDIREF$+DATA2,0)",
            "          LXM,U     X5,1",
            "          LXI,U     X5,3",
            "          LXM,U     X7,0",
            "          LXI,U     X7,1",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          LA        A3,DATA1,*X5",
            "          SA        A3,DATA2,*X7",
            "          HALT      0",
            "",
            "$(4)",
            "DATA1     +0",
            "          +01",
            "          +0",
            "          +0",
            "          +02",
            "          +0",
            "          +0",
            "          +03",
            "          +0",
            "          +0",
            "          +05",
            "          +0",
            "          +0",
            "          +010",
            "",
            "$(6)",
            "DATA2     $res      8",
            "",
            "          $end start"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        long[] bankData = getBankByBaseRegister(ip, 15);
        assertEquals(01, bankData[0]);
        assertEquals(02, bankData[1]);
        assertEquals(03, bankData[2]);
        assertEquals(05, bankData[3]);
        assertEquals(010, bankData[4]);
    }

    @Test
    public void storage_indirect_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LBU       B13,(LBDIREF$+DATA1,0)",
            "          LBU       B15,(LBDIREF$+DATA2,0)",
            "          LA        A0,*DATA1",
            "          HALT      0",
            "",
            "$(4)",
            "DATA2",
            "          NOP       *DATA1+1",
            "          +         011,022,033,044,055,066",
            "",
            "$(6)",
            "DATA1",
            "          NOP       *DATA2",
            "          NOP       *DATA1+2",
            "          NOP       *DATA1+3",
            "          NOP       *DATA1+4",
            "          NOP       DATA2+1",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_112233_445566L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    @Test
    public void execRegisterSelection_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          LD        (1,0) . exec regs",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          $INFO 1 5",
            "",
            "          LA,U      EA5,01",
            "          LX,U      EX5,05",
            "          LR,U      ER5,077",
            "          HALT      0",
            "",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        ip.getDesignatorRegister().setProcessorPrivilege(0);
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(01, ip.getGeneralRegister(GeneralRegisterSet.EA5).getW());
        Assert.assertEquals(05, ip.getGeneralRegister(GeneralRegisterSet.EX5).getW());
        Assert.assertEquals(077, ip.getGeneralRegister(GeneralRegisterSet.ER5).getW());
    }

    @Test
    public void storage_BasicMode(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (01,0),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0)",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LA        A0,DATA",
            "          HALT      0",
            "",
            "DATA      + 0112233,0445566",
            "          $END START"
        };

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(0_112233_445566L, ip.getGeneralRegister(GeneralRegisterSet.A0).getW());
    }

    //TODO read reference violation GAP

    //TODO write reference violation GAP

    //TODO execute reference violation GAP

    //TODO read reference violation SAP

    //TODO write reference violation SAP

    //TODO execute reference violation SAP

    //TODO reference out of limits BASIC mode
}
