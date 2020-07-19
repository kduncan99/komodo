/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_SpecialInstructions extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    @Test
    public void nop_basic(
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
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       040000,*X2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(4, ip.getGeneralRegister(2).getH1());
        assertEquals(6, ip.getGeneralRegister(2).getH2());
    }

    @Test
    public void nop_basic_indirect_violation(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BMSTART, BMSTART)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BMSTART",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       *040000,*X2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(01010, ip.getLatestStopDetail());
    }

    @Test
    public void nop_basic_indirect_noViolation(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BMSTART, BMSTART)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BMSTART",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       *DATA,*X2",
            "          HALT      0",
            "",
            "DATA      $RES      8",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(4, ip.getGeneralRegister(2).getH1());
        assertEquals(6, ip.getGeneralRegister(2).getH2());
    }

    @Test
    public void nop_extended(
    ) throws Exception {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          LD        (0)",
            "          LXI,U     X2,4",
            "          LXM,U     X2,2",
            "          NOP       04000,*X2,B2",
            "          HALT      0",
            "DATA      + 0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        assertEquals(0, ip.getLatestStopDetail());
        assertEquals(4, ip.getGeneralRegister(2).getH1());
        assertEquals(6, ip.getGeneralRegister(2).getH2());
    }
}
