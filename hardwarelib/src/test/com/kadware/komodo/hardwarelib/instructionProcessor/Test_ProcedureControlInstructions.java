/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_ProcedureControlInstructions extends BaseFunctions {

    @After
    public void after() {
        clear();
    }

    //TODO need unit tests for LBJ, LDJ, LIJ
    //TODO need unit tests for indirect banks, gate banks, etc

    @Test
    public void callNormal(
    ) throws Exception {
        String[] source = {
            "          $INFO     10 3,5",
            "",
            "          CALL      (LBDIREF$+TARGET3, TARGET3)",
            "          HALT      077 . should not get here",
            "",
            "$(3)      $LIT . won't be initially based",
            "TARGET3*",
            "          CALL      (LBDIREF$+TARGET5, TARGET5)",
            "          HALT      076 . or here",
            "",
            "$(5)           . also won't be initially based",
            "TARGET5*",
            "          HALT      0   . should land here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        //  The following depend on RCSTACK values set in BaseFunctions
        Assert.assertEquals(63, ip.getBaseRegister(25)._upperLimitNormalized);
        Assert.assertEquals(64 - 4, ip.getGeneralRegister(GeneralRegisterSet.EX0).getW());
    }

    @Test
    public void gotoNormal(
    ) throws Exception {
        String[] source = {
            "          $INFO     10 3,5",
            "",
            "          GOTO      (LBDIREF$+TARGET3, TARGET3)",
            "          HALT      077 . should not get here",
            "",
            "$(3)      $LIT . won't be initially based",
            "TARGET3*",
            "          GOTO      (LBDIREF$+TARGET5, TARGET5)",
            "          HALT      076 . or here",
            "",
            "$(5)           . also won't be initially based",
            "TARGET5*",
            "          HALT      0   . should land here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
    }

    @Test
    public void loclNormal(
    ) throws Exception {
        String[] source = {
            "          LOCL      TARGET1",
            "          HALT      077 . should not get here",
            "",
            "TARGET1 .",
            "          LXM,U     X5,TARGET2",
            "          LOCL      0,X5",
            "          HALT      076 . or here",
            "",
            "TARGET2 . ",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        //  The following depend on RCSTACK values set in BaseFunctions
        Assert.assertEquals(63, ip.getBaseRegister(25)._upperLimitNormalized);
        Assert.assertEquals(64 - 4, ip.getGeneralRegister(GeneralRegisterSet.EX0).getW());
    }

    @Test
    public void rtnToCall(
    ) throws Exception {
        String[] source = {
            "          $INFO 10 3",
            "",
            "          LA,U      A5,5",
            "          CALL      (LBDIREF$+TARGETSUB, TARGETSUB)",
            "          LA,U      A7,7",
            "          HALT      0 . should stop here",
            "",
            "$(3)",
            "TARGETSUB* .",
            "          LA,U      A6,6",
            "          RTN       0",
            "          HALT      077 . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(63, ip.getBaseRegister(25)._upperLimitNormalized);
        Assert.assertEquals(64, ip.getGeneralRegister(GeneralRegisterSet.EX0).getW());
        Assert.assertEquals(5, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        Assert.assertEquals(6, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        Assert.assertEquals(7, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
    }

    @Test
    public void rtnToLocl(
    ) throws Exception {
        String[] source = {
            "          LA,U      A5,5",
            "          LOCL      TARGETSUB",
            "          LA,U      A7,7",
            "          HALT      0 . should stop here",
            "",
            "TARGETSUB .",
            "          LA,U      A6,6",
            "          RTN       0",
            "          HALT      077 . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(63, ip.getBaseRegister(25)._upperLimitNormalized);
        Assert.assertEquals(64, ip.getGeneralRegister(GeneralRegisterSet.EX0).getW());
        Assert.assertEquals(5, ip.getGeneralRegister(GeneralRegisterSet.A5).getW());
        Assert.assertEquals(6, ip.getGeneralRegister(GeneralRegisterSet.A6).getW());
        Assert.assertEquals(7, ip.getGeneralRegister(GeneralRegisterSet.A7).getW());
    }

    @Test
    public void rtnNoFrame(
    ) throws Exception {
        String[] source = {
            "          RTN       0",
            "          HALT      077 . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01013, ip.getLatestStopDetail());
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }
}
